package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.domain.persistence.ReplaySweeperConfigurationRepository;
import net.warp_scores.warpscores.domain.persistence.ReplaySweeperLogRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.ReplayDownload;
import net.warp_scores.warpscores.model.ReplaySweeperConfiguration;
import net.warp_scores.warpscores.model.ReplaySweeperLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service @RequiredArgsConstructor
public class ReplaySweeperService {
    private final ReplaySweeperConfigurationRepository configurations;
    private final ReplayDownloadRepository downloads;
    private final MatchRepository matches;
    private final ReplaySweeperLogRepository logs;
    private final PyBb3Client pybb3;
    private final AtomicBoolean running=new AtomicBoolean();
    @Value("${replay-sweeper.cron:0 0 5 * * *}") private String defaultCron;
    @Value("${replay-sweeper.zone-id:Europe/Stockholm}") private String defaultZone;
    @Value("${replay-sweeper.batch-size:10}") private int defaultBatchSize;
    @Value("${replay-sweeper.storage-directory:./data/replays}") private String storageDirectory;

    public ReplaySweeperConfiguration get(){return configurations.findById("replay-sweeper").orElseGet(()->{var c=new ReplaySweeperConfiguration();c.setCron(defaultCron);c.setZoneId(defaultZone);c.setBatchSize(defaultBatchSize);return configurations.save(c);});}
    public ReplaySweeperConfiguration update(boolean enabled,String cron,String zoneId,int batchSize,String username){CronExpression.parse(cron);ZoneId.of(zoneId);var c=get();c.setEnabled(enabled);c.setCron(cron);c.setZoneId(zoneId);c.setBatchSize(Math.max(1,Math.min(batchSize,50)));c.setSteamUsername(blankToNull(username));return configurations.save(c);}
    public Map<String,Object> status(){var c=refreshCredentialStatus(get());return Map.ofEntries(Map.entry("enabled",c.isEnabled()),Map.entry("cron",c.getCron()),Map.entry("zoneId",c.getZoneId()),Map.entry("batchSize",c.getBatchSize()),Map.entry("steamUsername",Objects.toString(c.getSteamUsername(),"")),Map.entry("credentialConfigured",c.isCredentialConfigured()),Map.entry("credentialValid",c.isCredentialValid()),Map.entry("running",running.get()),Map.entry("lastStartedAt",Objects.toString(c.getLastStartedAt(),"")),Map.entry("lastCompletedAt",Objects.toString(c.getLastCompletedAt(),"")),Map.entry("lastError",Objects.toString(c.getLastError(),"")),Map.entry("lastDownloaded",c.getLastDownloaded()));}
    public List<ReplaySweeperLog> logs(){return logs.findTop50ByOrderByCreatedAtDesc();}
    public void credentialAuthenticated(String username){var c=get();c.setCredentialConfigured(true);c.setCredentialValid(true);c.setSteamUsername(username);c.setLastError(null);configurations.save(c);log("INFO","CREDENTIAL_RENEWED","Replay service Steam ticket renewed",null);}
    public boolean due(){var c=get();if(!c.isEnabled()||!c.isCredentialValid()||!c.isCredentialConfigured())return false;ZoneId zone=ZoneId.of(c.getZoneId());ZonedDateTime now=ZonedDateTime.now(zone);ZonedDateTime base=c.getLastStartedAt()==null?now.minusDays(1):ZonedDateTime.ofInstant(c.getLastStartedAt().toInstant(),zone);ZonedDateTime next=CronExpression.parse(c.getCron()).next(base);return next!=null&&!next.isAfter(now);}
    @Async
    public void run(){if(!running.compareAndSet(false,true))return;var c=get();c.setLastStartedAt(new Date());c.setLastError(null);configurations.save(c);log("INFO","SWEEP_STARTED","Scheduled replay sweep started",null);try{List<Match> candidates=matches.findAll(PageRequest.of(0,Math.max(c.getBatchSize()*20,200),Sort.by(Sort.Direction.DESC,"finished"))).getContent().stream().filter(m->m.getId()!=null&&m.getId().getOpus()==3&&m.getFinished()!=null&&m.getMatchId()!=null&&!m.getMatchId().isBlank()).filter(m->downloads.findById(m.getId().asMongoKey()).map(d->!"DOWNLOADED".equals(d.getStatus())).orElse(true)).limit(c.getBatchSize()).toList();if(candidates.isEmpty()){complete(c,0,null);log("INFO","NOTHING_TO_DO","No missing BB3 replays found",0);return;}Map<String,Object> response=pybb3.post("/api/v1/replays/batch","replay-sweeper",Map.of("credentialId","replay-sweeper","gameIds",candidates.stream().map(Match::getMatchId).toList()));int count=saveResults(candidates,response);complete(c,count,null);log("INFO","SWEEP_COMPLETED","Replay sweep completed",count);}catch(PyBb3ServiceException e){if(e.getStatusCode()==401){c.setCredentialValid(false);log("WARN","CREDENTIAL_INVALID","Replay service Steam ticket must be renewed",0);}else if(e.getStatusCode()==409){log("WARN","STEAM_ACCOUNT_ACTIVE","Steam account was active; replay sweep skipped",0);}else log("ERROR","PYBB3_ERROR",e.getMessage(),0);complete(c,0,e.getMessage());}catch(Exception e){complete(c,0,e.getMessage());log("ERROR","SWEEP_FAILED",Objects.toString(e.getMessage(),"Replay sweep failed"),0);}finally{running.set(false);}}
    private int saveResults(List<Match> candidates,Map<String,Object> response)throws Exception{Map<String,Match> byGame=new HashMap<>();candidates.forEach(m->byGame.put(m.getMatchId(),m));Object raw=response.get("results");if(!(raw instanceof List<?> list))return 0;Files.createDirectories(Path.of(storageDirectory));int saved=0;for(Object item:list)if(item instanceof Map<?,?> result){String game=Objects.toString(result.get("gameId"),"");Match match=byGame.get(game);if(match==null)continue;ReplayDownload record=downloads.findById(match.getId().asMongoKey()).orElseGet(ReplayDownload::new);record.setMatchId(match.getId().asMongoKey());record.setGameId(game);record.setAttemptedAt(new Date());Object data=result.get("data");if(data instanceof String encoded){String safe=game.replaceAll("[^A-Za-z0-9._-]","_");Path target=Path.of(storageDirectory,safe+".xml"),temp=Path.of(storageDirectory,safe+".tmp");Files.write(temp,Base64.getDecoder().decode(encoded));Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);record.setFileName(target.toString());record.setStatus("DOWNLOADED");record.setDownloadedAt(new Date());record.setError(null);saved++;}else{record.setStatus("FAILED");record.setError(Objects.toString(result.get("error"),"Replay unavailable"));}downloads.save(record);}return saved;}
    private void complete(ReplaySweeperConfiguration c,int downloaded,String error){c.setLastCompletedAt(new Date());c.setLastDownloaded(downloaded);c.setLastError(error);configurations.save(c);}
    private ReplaySweeperConfiguration refreshCredentialStatus(ReplaySweeperConfiguration c){try{boolean previouslyConfigured=c.isCredentialConfigured();Map<String,Object>s=pybb3.get("/api/v1/replays/credentials/replay-sweeper","replay-sweeper");c.setCredentialConfigured(Boolean.TRUE.equals(s.get("credentialConfigured")));if(!c.isCredentialConfigured())c.setCredentialValid(false);else if(!previouslyConfigured)c.setCredentialValid(true);String username=Objects.toString(s.get("username"),"");if(!username.isBlank())c.setSteamUsername(username);return configurations.save(c);}catch(RuntimeException ignored){return c;}}
    private void log(String level,String code,String message,Integer downloaded){ReplaySweeperLog entry=new ReplaySweeperLog();entry.setId(UUID.randomUUID().toString());entry.setCreatedAt(new Date());entry.setLevel(level);entry.setCode(code);entry.setMessage(message);entry.setDownloaded(downloaded);logs.save(entry);}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
}
