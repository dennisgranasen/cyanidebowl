package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.ReplaySweeperService;
import net.warp_scores.warpscores.service.ReplayAnalysisBackfillService;
import net.warp_scores.warpscores.service.ReplayArtifactService;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.IdentityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ReplayDownloadRepository downloads;
    private final ReplayAnalysisBackfillService analysis;
    private final MatchRepository matches;
    private final ReplayArtifactService replayArtifacts;
    @Value("${replay-sweeper.availability-window-days:30}") private int availabilityWindowDays;
    @GetMapping public Map<String,Object> status(){return service.status();}
    @GetMapping("/logs") public Object logs(){return service.logs();}
    @PutMapping public Map<String,Object> update(@RequestBody Settings value){service.update(value.enabled(),value.cron(),value.zoneId(),value.batchSize(),value.steamUsername());return service.status();}
    @PostMapping("/run") public Map<String,Object> run(){boolean accepted=service.run();var status=new HashMap<>(service.status());status.put("accepted",accepted);return status;}
    @GetMapping("/replays") public Object replays(){var cutoff=java.util.Date.from(java.time.Instant.now().minus(java.time.Duration.ofDays(Math.max(1,availabilityWindowDays))));return downloads.findTop50ByAttemptedAtAfterOrderByAttemptedAtDesc(cutoff).stream().map(replay->{
        var result=new java.util.LinkedHashMap<String,Object>();
        result.put("matchId",replay.getMatchId());result.put("gameId",replay.getGameId());
        result.put("status",replay.getStatus());result.put("analysisStatus",replay.getAnalysisStatus());
        result.put("downloadedAt",replay.getDownloadedAt());result.put("originalSize",replay.getOriginalSize());
        result.put("compactSize",replay.getCompactSize());result.put("error",replay.getError());
        result.put("analysisError",replay.getAnalysisError());
        result.put("originalFormat",replay.getOriginalFormat());
        result.put("availabilityWindowDays",availabilityWindowDays);
        try{matches.findById(IdentityUtil.fromId(replay.getMatchId())).ifPresent(match->{
            result.put("playedAt",match.getFinished());result.put("competitionName",match.getCompetitionName());
            if(match.getTeams()!=null)result.put("teams",java.util.Arrays.stream(match.getTeams()).filter(java.util.Objects::nonNull).map(team->team.getName()).toList());
        });}catch(RuntimeException ignored){}
        return result;
    }).toList();}
    @PostMapping("/replays/{matchId}/analyze") public Map<String,Object> analyze(@PathVariable String matchId){analysis.analyze(matchId);return Map.of("matchId",matchId,"status","PROCESSED");}
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
