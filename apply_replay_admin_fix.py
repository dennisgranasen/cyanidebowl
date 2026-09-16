from pathlib import Path
import re
import sys

ROOT = Path.cwd()


def load(path):
    p = ROOT / path
    if not p.exists():
        raise SystemExit(f"Missing expected file: {path}")
    return p, p.read_text(encoding="utf-8")


def save(p, text):
    p.write_text(text, encoding="utf-8")
    print(f"updated {p.relative_to(ROOT)}")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def regex_once(text, pattern, replacement, label, flags=0):
    new, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return new


# 1. Efficient page query: recent BB3 matches, sorted/paged in Mongo.
path = "backend/src/main/java/net/warp_scores/warpscores/domain/persistence/MatchRepository.java"
p, text = load(path)
old = "    List<Match> findAllById(List<Identity> matchIds);\n"
new = old + "\n    @Query(\"{ '_id.opus': 3, 'finished': { '$ne': null }, 'matchId': { '$nin': [null, ''] } }\")\n    List<Match> findReplayAdminMatches(Pageable pageable);\n"
text = replace_once(text, old, new, "MatchRepository query")
save(p, text)

# 2. Bulk reanalysis is explicit admin work, not part of normal page load.
path = "backend/src/main/java/net/warp_scores/warpscores/service/ReplayAnalysisBackfillService.java"
p, text = load(path)
if "import org.springframework.data.domain.Sort;" not in text:
    text = replace_once(
        text,
        "import org.springframework.data.domain.PageRequest;\n",
        "import org.springframework.data.domain.PageRequest;\nimport org.springframework.data.domain.Sort;\n",
        "ReplayAnalysisBackfillService Sort import",
    )
marker = "    public QueueSnapshot snapshot() {\n"
bulk = '''    public BulkReanalysisResult requestReanalysisAll(String requestedBy) {
        String actor = requestedBy == null || requestedBy.isBlank() ? "site-admin" : requestedBy;
        int queued = 0;
        int missingLocal = 0;
        int unavailable = 0;

        for (int page = 0; ; page++) {
            var records = downloads.findAll(PageRequest.of(
                    page,
                    100,
                    Sort.by(Sort.Direction.ASC, "matchId"))).getContent();
            if (records.isEmpty()) break;

            var changed = new ArrayList<net.warp_scores.warpscores.model.ReplayDownload>();
            for (var record : records) {
                if (!"DOWNLOADED".equals(record.getStatus())) {
                    unavailable++;
                    continue;
                }
                if (!artifacts.originalAvailable(record)) {
                    missingLocal++;
                    continue;
                }
                record.setAnalysisRequestedAt(new Date());
                record.setAnalysisRequestedBy(actor);
                record.setAnalysisError(null);
                changed.add(record);
                queued++;
            }
            if (!changed.isEmpty()) downloads.saveAll(changed);
            if (records.size() < 100) break;
        }

        return new BulkReanalysisResult(queued, missingLocal, unavailable);
    }

    public record BulkReanalysisResult(int queued, int missingLocal, int unavailable) {}

'''
if "requestReanalysisAll(" not in text:
    text = replace_once(text, marker, bulk + marker, "bulk reanalysis insertion")
save(p, text)

# 3. Replay admin endpoint: page from Match documents directly. This avoids reparsing
#    Mongo keys such as 3-<uuid>, where UUID '-' characters made IdentityUtil build a
#    CompositeIdentity and therefore caused every metadata lookup to miss.
path = "backend/src/main/java/net/warp_scores/warpscores/controller/ReplaySweeperAdminController.java"
p, text = load(path)
text = text.replace("import net.warp_scores.warpscores.identity.IdentityUtil;\n", "")
for imp in [
    "import net.warp_scores.warpscores.model.Match;\n",
    "import net.warp_scores.warpscores.model.ReplayDownload;\n",
    "import org.springframework.data.domain.PageRequest;\n",
    "import org.springframework.data.domain.Sort;\n",
]:
    if imp not in text:
        text = replace_once(text, "import net.warp_scores.warpscores.domain.persistence.MatchRepository;\n", "import net.warp_scores.warpscores.domain.persistence.MatchRepository;\n" + imp, f"controller import {imp.strip()}")

replacement = '''    @GetMapping("/replays")
    public Map<String,Object> replays(@RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        // One extra row tells the UI whether an older page exists without a count query.
        var recent = matches.findReplayAdminMatches(PageRequest.of(
                safePage,
                safeSize + 1,
                Sort.by(Sort.Direction.DESC, "finished")));
        boolean hasMore = recent.size() > safeSize;
        var pageMatches = hasMore ? recent.subList(0, safeSize) : recent;

        var replayIds = pageMatches.stream()
                .filter(match -> match.getId() != null)
                .map(match -> match.getId().asMongoKey())
                .toList();
        var replayById = new java.util.HashMap<String, ReplayDownload>();
        if (!replayIds.isEmpty()) {
            downloads.findAllById(replayIds)
                    .forEach(replay -> replayById.put(replay.getMatchId(), replay));
        }

        var items = pageMatches.stream()
                .map(match -> replayRow(match, replayById.get(match.getId().asMongoKey())))
                .toList();

        return Map.of(
                "items", items,
                "page", safePage,
                "size", safeSize,
                "hasPrevious", safePage > 0,
                "hasMore", hasMore);
    }

    private Map<String,Object> replayRow(Match match, ReplayDownload replay) {
        var result = new java.util.LinkedHashMap<String,Object>();
        String matchId = match.getId().asMongoKey();
        result.put("matchId", matchId);
        result.put("gameId", replay == null ? match.getMatchId() : replay.getGameId());
        result.put("status", replay == null ? "NOT_DOWNLOADED" : replay.getStatus());
        result.put("analysisStatus", replay == null ? null : replay.getAnalysisStatus());
        result.put("parserVersion", replay == null ? null : replay.getParserVersion());
        result.put("analysisAttemptVersion", replay == null ? null : replay.getAnalysisAttemptVersion());
        result.put("analysisRequestedAt", replay == null ? null : replay.getAnalysisRequestedAt());
        result.put("downloadedAt", replay == null ? null : replay.getDownloadedAt());
        result.put("originalSize", replay == null ? null : replay.getOriginalSize());
        result.put("compactSize", replay == null ? null : replay.getCompactSize());
        result.put("error", replay == null ? null : replay.getError());
        result.put("analysisError", replay == null ? null : replay.getAnalysisError());
        result.put("originalFormat", replay == null ? null : replay.getOriginalFormat());
        result.put("originalAvailable", replay != null && replayArtifacts.originalAvailable(replay));
        result.put("compactAvailable", replay != null && replayArtifacts.compactAvailable(replay));
        result.put("availabilityWindowDays", availabilityWindowDays);
        result.put("playedAt", match.getFinished());
        result.put("competitionName", match.getCompetitionName());
        if (match.getTeams() != null) {
            result.put("teams", java.util.Arrays.stream(match.getTeams())
                    .filter(java.util.Objects::nonNull)
                    .map(team -> team.getName())
                    .toList());
        }
        return result;
    }

    @GetMapping("/analysis-queue")'''
text = regex_once(
    text,
    r'    @GetMapping\("/replays"\) public Object replays\(\)\{.*?\n    @GetMapping\("/analysis-queue"\)',
    replacement,
    "replace replay list endpoint",
    re.S,
)
queue_line = '    @GetMapping("/analysis-queue") public Object analysisQueue(){return analysis.snapshot();}\n'
if "replays/analyze-all" not in text:
    text = replace_once(
        text,
        queue_line,
        queue_line
        + '    @PostMapping("/replays/analyze-all")\n'
        + '    public ReplayAnalysisBackfillService.BulkReanalysisResult analyzeAll(Authentication authentication) {\n'
        + '        return analysis.requestReanalysisAll(authentication == null ? null : authentication.getName());\n'
        + '    }\n',
        "analyze-all endpoint",
    )
save(p, text)

# 4. Frontend API methods.
path = "frontend/src/WarpScoresApiService.jsx"
p, text = load(path)
text = replace_once(
    text,
    "  replaySweeperReplays: async (getAccessTokenSilently, getAccessTokenWithPopup) =>\n"
    "    getDataWithAuthentication('/admin/replay-sweeper/replays', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),\n",
    "  replaySweeperReplays: async (page, size, getAccessTokenSilently, getAccessTokenWithPopup) =>\n"
    "    getDataWithAuthentication(`/admin/replay-sweeper/replays?page=${page}&size=${size}`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),\n",
    "replaySweeperReplays API",
)
needle = "  analyzeReplay: async (matchId, getAccessTokenSilently, getAccessTokenWithPopup) =>\n    postDataWithAuthentication(`/admin/replay-sweeper/replays/${encodeURIComponent(matchId)}/analyze`, {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),\n"
if "analyzeAllReplays:" not in text:
    text = replace_once(
        text,
        needle,
        needle + "  analyzeAllReplays: async (getAccessTokenSilently, getAccessTokenWithPopup) =>\n    postDataWithAuthentication('/admin/replay-sweeper/replays/analyze-all', {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),\n",
        "analyzeAllReplays API",
    )
save(p, text)

# 5. Replay admin UI: 10 rows/page, newer/older navigation, explicit analyze-all.
path = "frontend/src/components/admin/ReplaySweeperAdmin.jsx"
p, text = load(path)
state_pattern = r"  const\[status,setStatus\]=useState\(null\),\[logs,setLogs\]=useState\(\[\]\),\[replays,setReplays\]=useState\(\[\]\),(.*?)\[error,setError\]=useState\(''\);\n  const load=\(\)=>Promise\.all\(\[WarpScoresApiService\.replaySweeperStatus\(\.\.\.auth\),WarpScoresApiService\.replaySweeperLogs\(\.\.\.auth\),WarpScoresApiService\.replaySweeperReplays\(\.\.\.auth\)\]\)\n    \.then\(\(\[s,l,r\]\)=>\{setStatus\(s\);setLogs\(l\);setReplays\(r\);setError\(''\);return s\}\)"
state_repl = "  const[status,setStatus]=useState(null),[logs,setLogs]=useState([]),[replays,setReplays]=useState([]),[replayPage,setReplayPage]=useState(0),[replayPageInfo,setReplayPageInfo]=useState({hasPrevious:false,hasMore:false}),[files,setFiles]=useState([]),[importResults,setImportResults]=useState([]),[inspect,setInspect]=useState(null),[password,setPassword]=useState(''),[challenge,setChallenge]=useState(null),[code,setCode]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('');\n  const load=()=>Promise.all([WarpScoresApiService.replaySweeperStatus(...auth),WarpScoresApiService.replaySweeperLogs(...auth),WarpScoresApiService.replaySweeperReplays(replayPage,10,...auth)])\n    .then(([s,l,r])=>{setStatus(s);setLogs(l);setReplays(r?.items||[]);setReplayPageInfo({hasPrevious:Boolean(r?.hasPrevious),hasMore:Boolean(r?.hasMore)});setError('');return s})"
text = regex_once(text, state_pattern, state_repl, "replay admin state/load")
text = replace_once(text, "  },[]); // eslint-disable-line react-hooks/exhaustive-deps\n", "  },[replayPage]); // eslint-disable-line react-hooks/exhaustive-deps\n", "replay polling page dependency")
run_old = "  const run=async action=>{setBusy(true);setError('');try{return await action()}catch(e){setError(e?.response?.data?.message||e.message)}finally{setPassword('');setBusy(false)}};\n"
run_new = "  const run=async action=>{setBusy(true);setError('');setNotice('');try{return await action()}catch(e){setError(e?.response?.data?.message||e.message)}finally{setPassword('');setBusy(false)}};\n  const analyzeAll=()=>{if(!window.confirm(intl.formatMessage({id:'replay.analyzeAllConfirm',defaultMessage:'Queue reanalysis for every locally available replay?'})))return;run(()=>WarpScoresApiService.analyzeAllReplays(...auth).then(result=>{setNotice(intl.formatMessage({id:'replay.analyzeAllQueued',defaultMessage:'Queued {count} replays for analysis.'},{count:result.queued}));return load()}));};\n"
text = replace_once(text, run_old, run_new, "analyze all UI action")
text = replace_once(text, "    {error&&<Alert status=\"error\" mb={4}><AlertIcon/>{error}</Alert>}\n", "    {error&&<Alert status=\"error\" mb={4}><AlertIcon/>{error}</Alert>}\n    {notice&&<Alert status=\"success\" mb={4}><AlertIcon/>{notice}</Alert>}\n", "success notice")
text = replace_once(
    text,
    "    <Text fontSize=\"sm\" color=\"gray.500\" mb={2}>{intl.formatMessage({id:'replay.windowHelp'})}</Text>\n",
    "    <HStack justify=\"space-between\" align=\"center\" mb={2}>\n"
    "      <Text fontSize=\"sm\" color=\"gray.500\">{intl.formatMessage({id:'replay.recentHelp',defaultMessage:'Showing the 10 most recently played BB3 matches. Browse to older matches when you need to inspect or reanalyse them.'})}</Text>\n"
    "      <Button size=\"sm\" colorScheme=\"orange\" variant=\"outline\" isDisabled={busy} onClick={analyzeAll}>{intl.formatMessage({id:'replay.analyzeAll',defaultMessage:'Analyze all'})}</Button>\n"
    "    </HStack>\n",
    "recent replay help",
)
text = regex_once(
    text,
    r'(    <Box overflowX="auto"><Table size="sm">.*?</Table></Box>)\n(    <Heading size="sm" mt=\{5\} mb=\{2\}>\{intl\.formatMessage\(\{id:\'replay\.adminLog\'\}\)\}</Heading>)',
    r'''\1
    <HStack justify="space-between" mt={3}>
      <Button size="sm" isDisabled={busy||!replayPageInfo.hasPrevious} onClick={()=>setReplayPage(page=>Math.max(0,page-1))}>{intl.formatMessage({id:'common.previous'})}</Button>
      <Text fontSize="sm" color="gray.500">{intl.formatMessage({id:'replay.page',defaultMessage:'Page {page}'},{page:replayPage+1})}</Text>
      <Button size="sm" isDisabled={busy||!replayPageInfo.hasMore} onClick={()=>setReplayPage(page=>page+1)}>{intl.formatMessage({id:'common.next'})}</Button>
    </HStack>
\2''',
    "pagination controls",
    re.S,
)
save(p, text)

print("\nReplay admin fix applied. Review with: git diff --check && git diff")
