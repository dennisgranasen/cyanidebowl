package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.ReplaySweeperService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @GetMapping public Map<String,Object> status(){return service.status();}
    @GetMapping("/logs") public Object logs(){return service.logs();}
    @PutMapping public Map<String,Object> update(@RequestBody Settings value){service.update(value.enabled(),value.cron(),value.zoneId(),value.batchSize(),value.steamUsername());return service.status();}
    @PostMapping("/run") public Map<String,Object> run(){service.run();return service.status();}
    @PostMapping("/auth") public Map<String,Object> auth(@RequestBody Login value){return accept(pybb3.post("/api/v1/auth/start",OWNER,Map.of("username",value.username(),"password",value.password(),"persistCredential",true)));}
    @PostMapping("/challenges/{id}/code") public Map<String,Object> code(@PathVariable String id,@RequestBody Code value){return accept(pybb3.post("/api/v1/auth/challenges/"+id+"/code",OWNER,Map.of("code",value.code())));}
    @PostMapping("/challenges/{id}/confirm") public Map<String,Object> confirm(@PathVariable String id){return accept(pybb3.post("/api/v1/auth/challenges/"+id+"/confirm",OWNER,Map.of()));}
    private Map<String,Object> accept(Map<String,Object> result){var safe=new HashMap<>(result);safe.remove("credential");safe.remove("sessionId");if("AUTHENTICATED".equals(result.get("status")))service.credentialAuthenticated((String)result.get("steamUsername"));return safe;}
    public record Settings(boolean enabled,String cron,String zoneId,int batchSize,String steamUsername){}
    public record Login(String username,String password){}
    public record Code(String code){}
}
