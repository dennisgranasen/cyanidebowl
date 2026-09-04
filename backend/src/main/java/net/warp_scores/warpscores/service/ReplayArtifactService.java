package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.model.ReplayDownload;
import net.warp_scores.warpscores.model.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
public class ReplayArtifactService {
    public static final int PARSER_VERSION = 1;

    private final ReplayDownloadRepository downloads;
    private final ReplayAnalysisRepository analyses;
    private final MatchRepository matches;
    private final ObjectMapper mapper;
    private final PyBb3Client pybb3;

    @Value("${replay-sweeper.storage-directory:./data/replays}")
    private String storageDirectory;

    public record OriginalReplay(String fileName, byte[] data) {}
    public record ImportedReplay(String fileName, String sourceMatchId, String matchId, boolean imported, String error) {}

    public OriginalReplay readOriginal(String matchId) throws Exception {
        ReplayDownload record = downloads.findById(matchId)
                .filter(value -> "DOWNLOADED".equals(value.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("No replay exists for match " + matchId));
        String name = record.getOriginalFileName() == null ? record.getFileName() : record.getOriginalFileName();
        if (name == null) throw new IllegalArgumentException("Replay file is missing");
        Path root = Path.of(storageDirectory).toAbsolutePath().normalize();
        Path source = Path.of(name).toAbsolutePath().normalize();
        if (!source.startsWith(root)) throw new IllegalStateException("Replay path is outside configured storage");
        return new OriginalReplay(source.getFileName().toString(), Files.readAllBytes(source));
    }

    public byte[] readCompactJson(String matchId) throws Exception {
        ReplayDownload record = downloads.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("No replay exists for match " + matchId));
        if (record.getCompactFileName() == null) throw new IllegalArgumentException("Compact replay is missing");
        Path root = Path.of(storageDirectory).toAbsolutePath().normalize();
        Path source = Path.of(record.getCompactFileName()).toAbsolutePath().normalize();
        if (!source.startsWith(root)) throw new IllegalStateException("Replay path is outside configured storage");
        try (var input = new GZIPInputStream(Files.newInputStream(source))) { return input.readAllBytes(); }
    }

    public ImportedReplay importBbr(String suppliedName, byte[] bbr) {
        try {
            Map<String, Object> response = pybb3.post("/api/v1/replays/analyze", "replay-importer",
                    Map.of("data", Base64.getEncoder().encodeToString(bbr)));
            ReplayAnalysis parsed = mapper.convertValue(response.get("analysis"), ReplayAnalysis.class);
            String sourceMatchId = parsed.getSourceMatchId();
            if (sourceMatchId == null || sourceMatchId.isBlank())
                return new ImportedReplay(suppliedName, null, null, false, "Replay contains no match identifier");
            Match match = matches.findFirstByMatchId(sourceMatchId).orElse(null);
            if (match == null && suppliedName != null) {
                String fileId = Path.of(suppliedName).getFileName().toString().replaceFirst("(?i)\\.bbr$", "");
                match = matches.findFirstByMatchId(fileId).orElse(null);
                if (match != null) sourceMatchId = fileId;
            }
            if (match == null || match.getId() == null)
                return new ImportedReplay(suppliedName, sourceMatchId, null, false, "No BlaskScore match matches this replay");
            Map<String, Object> normalized = new HashMap<>(response);
            normalized.put("data", response.get("originalData"));
            normalized.put("fileName", suppliedName);
            boolean imported = storeDownloaded(match.getId().asMongoKey(), sourceMatchId, normalized);
            return new ImportedReplay(suppliedName, sourceMatchId, match.getId().asMongoKey(), imported,
                    imported ? null : "Replay could not be stored");
        } catch (Exception error) {
            return new ImportedReplay(suppliedName, null, null, false,
                    Objects.toString(error.getMessage(), "Replay could not be imported"));
        }
    }

    public boolean storeDownloaded(String matchId, String gameId, Map<?, ?> result) {
        ReplayDownload record = downloads.findById(matchId).orElseGet(ReplayDownload::new);
        record.setMatchId(matchId);
        record.setGameId(gameId);
        record.setAttemptedAt(new Date());
        try {
            Object encoded = result.get("data");
            Object compactEncoded = result.get("compactData");
            if (!(encoded instanceof String original) || !(compactEncoded instanceof String compact)) {
                record.setStatus("FAILED");
                record.setError(Objects.toString(result.get("error"), "Replay unavailable"));
                downloads.save(record);
                return false;
            }
            Path directory = Path.of(storageDirectory);
            Files.createDirectories(directory);
            String requestedName = Objects.toString(result.get("fileName"), gameId + ".bbr");
            String safe = Path.of(requestedName).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
            if (!safe.toLowerCase().endsWith(".bbr")) safe += ".bbr";
            Path originalPath = atomicWrite(directory.resolve(safe), Base64.getDecoder().decode(original));
            Path compactPath = atomicWrite(directory.resolve(gameId.replaceAll("[^A-Za-z0-9._-]", "_") + ".json.gz"), Base64.getDecoder().decode(compact));
            record.setFileName(originalPath.toString());
            record.setOriginalFileName(originalPath.toString());
            record.setCompactFileName(compactPath.toString());
            record.setOriginalSize(Files.size(originalPath));
            record.setCompactSize(Files.size(compactPath));
            record.setOriginalSha256(Objects.toString(result.get("originalSha256"), null));
            record.setOriginalFormat(Objects.toString(result.get("originalFormat"), "BBR"));
            record.setCompactSha256(Objects.toString(result.get("compactSha256"), null));
            record.setStatus("DOWNLOADED");
            record.setDownloadedAt(new Date());
            record.setError(null);
            storeAnalysis(record, result.get("analysis"));
            downloads.save(record);
            return true;
        } catch (Exception error) {
            record.setStatus("FAILED");
            record.setError(Objects.toString(error.getMessage(), "Replay artifact could not be stored"));
            downloads.save(record);
            return false;
        }
    }

    public void reanalyze(ReplayDownload record) throws Exception {
        String name = record.getOriginalFileName() == null ? record.getFileName() : record.getOriginalFileName();
        if (name == null) throw new IllegalStateException("Replay file path is missing");
        Path source = Path.of(name);
        byte[] raw = Files.readAllBytes(source);
        record.setAnalysisStatus("PROCESSING");
        record.setAnalysisError(null);
        downloads.save(record);
        Map<String, Object> response = pybb3.post("/api/v1/replays/analyze", "replay-analyzer",
                Map.of("data", Base64.getEncoder().encodeToString(raw)));
        Map<String, Object> normalized = new HashMap<>(response);
        normalized.put("data", response.get("originalData"));
        if (!storeDownloaded(record.getMatchId(), record.getGameId(), normalized))
            throw new IllegalStateException("Reprocessed replay artifacts could not be stored");
    }

    private void storeAnalysis(ReplayDownload record, Object rawAnalysis) {
        if (rawAnalysis == null) {
            record.setAnalysisStatus("PENDING");
            return;
        }
        try {
            ReplayAnalysis analysis = mapper.convertValue(rawAnalysis, ReplayAnalysis.class);
            analysis.setMatchId(record.getMatchId());
            analysis.setGameId(record.getGameId());
            analysis.setProcessedAt(new Date());
            matches.findById(IdentityUtil.fromId(record.getMatchId())).ifPresent(match -> enrich(analysis, match));
            analyses.save(analysis);
            record.setAnalysisStatus("PROCESSED");
            record.setParserVersion(analysis.getParserVersion());
            record.setAnalyzedAt(new Date());
            record.setAnalysisError(null);
        } catch (RuntimeException error) {
            record.setAnalysisStatus("FAILED");
            record.setAnalysisError(Objects.toString(error.getMessage(), "Replay analysis could not be stored"));
        }
    }

    private void enrich(ReplayAnalysis analysis, Match match) {
        enrichFacts(analysis.getDiceRolls(), match);
        enrichFacts(analysis.getResourceEvents(), match);
        enrichFacts(analysis.getSpecialEvents(), match);
        Map<String, Map<String, Object>> totals = new LinkedHashMap<>();
        if (analysis.getDiceRolls() != null) for (Map<String, Object> roll : analysis.getDiceRolls()) {
            Map<String, Object> participant = participant(totals, roll);
            increment(participant, "diceRolls", 1);
            Object rawDice = roll.get("dice");
            if (rawDice instanceof List<?> dice) for (Object rawDie : dice) if (rawDie instanceof Map<?, ?> die) {
                String face = Objects.toString(die.get("value"), "unknown");
                increment(participant, "face" + face, 1);
            }
        }
        addEventTotals(totals, analysis.getResourceEvents(), "resource:");
        addEventTotals(totals, analysis.getSpecialEvents(), "special:");
        analysis.setParticipantTotals(new ArrayList<>(totals.values()));
    }

    private void enrichFacts(List<Map<String, Object>> facts, Match match) {
        if (facts == null || match.getTeams() == null) return;
        for (Map<String, Object> fact : facts) {
            int index = teamIndex(fact.get("teamId"));
            if (index < 0 || index >= match.getTeams().length) continue;
            Team team = match.getTeams()[index];
            fact.put("sourceTeamId", fact.get("teamId"));
            fact.put("teamIndex", index);
            if (team != null && team.getId() != null) fact.put("teamId", team.getId().asMongoKey());
            String coachId = team != null && team.getCoachId() != null ? team.getCoachId().getValue()
                    : match.getCoaches() != null && index < match.getCoaches().length && match.getCoaches()[index] != null
                    ? match.getCoaches()[index].getId() : null;
            String coachName = team != null && team.getCoachName() != null ? team.getCoachName()
                    : match.getCoaches() != null && index < match.getCoaches().length && match.getCoaches()[index] != null
                    ? match.getCoaches()[index].getName() : null;
            if (coachId != null) fact.put("coachId", coachId);
            if (coachName != null) fact.put("coachName", coachName);
        }
    }

    private int teamIndex(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(Objects.toString(value)); }
        catch (Exception ignored) { return -1; }
    }

    private Map<String, Object> participant(Map<String, Map<String, Object>> totals, Map<String, Object> fact) {
        String coachId = Objects.toString(fact.get("coachId"), "unknown");
        String teamId = Objects.toString(fact.get("teamId"), "unknown");
        return totals.computeIfAbsent(coachId + ":" + teamId, ignored -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("coachId", fact.get("coachId")); value.put("coachName", fact.get("coachName"));
            value.put("teamId", fact.get("teamId")); return value;
        });
    }

    private void addEventTotals(Map<String, Map<String, Object>> totals, List<Map<String, Object>> facts, String prefix) {
        if (facts == null) return;
        for (Map<String, Object> fact : facts)
            increment(participant(totals, fact), prefix + Objects.toString(fact.get("eventType"), "unknown"), 1);
    }

    private void increment(Map<String, Object> target, String key, int amount) {
        target.put(key, ((Number) target.getOrDefault(key, 0)).intValue() + amount);
    }

    private Path atomicWrite(Path target, byte[] data) throws Exception {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, data);
        try {
            return Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            return Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
