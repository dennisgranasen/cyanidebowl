package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.ReplaySweeperService;
import net.warp_scores.warpscores.service.ReplayAnalysisBackfillService;
import net.warp_scores.warpscores.service.ReplayArtifactService;
import net.warp_scores.warpscores.service.FetchDataService;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import net.warp_scores.warpscores.model.ReplayDownload;
import net.warp_scores.warpscores.model.Match;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController @RequestMapping("/admin/replay-sweeper") @RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class ReplaySweeperAdminController {
    private static final String OWNER="replay-sweeper";
    private final ReplaySweeperService service;
    private final PyBb3Client pybb3;
    private final FetchDataService fetchDataService;
    private final ReplayDownloadRepository downloads;
    private final ReplayAnalysisBackfillService analysis;
    private final MatchRepository matches;
    private final ReplayArtifactService replayArtifacts;
    @Value("${replay-sweeper.availability-window-days:30}") private int availabilityWindowDays;
    @GetMapping public Map<String,Object> status(){return service.status();}
    @GetMapping("/logs") public Object logs(){return service.logs();}
    @PutMapping public Map<String,Object> update(@RequestBody Settings value){service.update(value.enabled(),value.cron(),value.zoneId(),value.batchSize(),value.steamUsername());return service.status();}
    @PostMapping("/run") public Map<String,Object> run(){boolean accepted=service.run();var status=new HashMap<>(service.status());status.put("accepted",accepted);return status;}
    @PostMapping("/scan-matches") public Map<String,Object> scanMatches(){fetchDataService.fetchNewMatches();return Map.of("status","COMPLETED");}
    @GetMapping("/replays")
    public Map<String,Object> replays(@RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        var recent = matches.findReplayAdminMatches(PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "finished")));
        boolean hasMore = recent.hasNext();
        var pageMatches = recent.getContent();

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

    @GetMapping("/analysis-queue") public Object analysisQueue(){return analysis.snapshot();}
    @PostMapping("/replays/analyze-all")
    public ReplayAnalysisBackfillService.BulkReanalysisResult analyzeAll(Authentication authentication) {
        return analysis.requestReanalysisAll(authentication == null ? null : authentication.getName());
    }
    @PostMapping("/replays/{matchId}/analyze") public Map<String,Object> analyze(@PathVariable String matchId, Authentication authentication){analysis.requestReanalysis(matchId,authentication==null?null:authentication.getName());return Map.of("matchId",matchId,"status","QUEUED");}
    @GetMapping(value="/replays/{matchId}/inspect",produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> inspect(@PathVariable String matchId){try{return ResponseEntity.ok(replayArtifacts.readCompactJson(matchId));}catch(IllegalArgumentException error){return ResponseEntity.notFound().build();}catch(Exception error){return ResponseEntity.internalServerError().build();}}
    @PostMapping(value="/replays/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object importReplays(@RequestPart("files") java.util.List<MultipartFile> files){
        if(files.size()>100)throw new IllegalArgumentException("At most 100 replay files can be imported at once");
        return files.stream().map(file->{try{if(file.getSize()>20L*1024*1024)return new ReplayArtifactService.ImportedReplay(file.getOriginalFilename(),null,null,false,"File exceeds 20 MiB");return replayArtifacts.importBbr(file.getOriginalFilename(),file.getBytes());}catch(Exception error){return new ReplayArtifactService.ImportedReplay(file.getOriginalFilename(),null,null,false,error.getMessage());}}).toList();
    }
    @PostMapping("/auth") public Map<String,Object> auth(@RequestBody Login value){return accept(pybb3.post("/api/v1/auth/start",OWNER,Map.of("username",value.username(),"password",value.password(),"persistCredential",true)));}
    @PostMapping("/challenges/{id}/code") public Map<String,Object> code(@PathVariable String id,@RequestBody Code value){return accept(pybb3.post("/api/v1/auth/challenges/"+id+"/code",OWNER,Map.of("code",value.code())));}
    @PostMapping("/challenges/{id}/confirm") public Map<String,Object> confirm(@PathVariable String id){return accept(pybb3.post("/api/v1/auth/challenges/"+id+"/confirm",OWNER,Map.of()));}
    private Map<String,Object> accept(Map<String,Object> result){var safe=new HashMap<>(result);safe.remove("credential");safe.remove("sessionId");if("AUTHENTICATED".equals(result.get("status")))service.credentialAuthenticated((String)result.get("steamUsername"));return safe;}
    public record Settings(boolean enabled,String cron,String zoneId,int batchSize,String steamUsername){}
    public record Login(String username,String password){}
    public record Code(String code){}
}
