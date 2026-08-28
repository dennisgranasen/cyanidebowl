package net.warp_scores.warpscores.controller;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLegEntity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LadderOption;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.requests.CircuitLegRequest;
import net.warp_scores.warpscores.service.CircuitService;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.utils.EnumUtils;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CircuitController {

    private final CircuitService circuitService;

    @GetMapping("/circuits")
    public ResponseEntity<List<Circuit>> getCircuits() {
        try {
            List<Circuit> circuits = circuitService.loadAll();

            return ResponseEntity.ok(circuits);
        } catch (Exception ex) {
            log.error("Unable to retrieve circuits", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/circuits/{circuitId}")
    public ResponseEntity<Circuit> getCircuit(
            @PathVariable(name = "circuitId") Long circuitId) {
        try {
            Optional<Circuit> circuit = circuitService.load(circuitId);
            return circuit
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to retrieve circuit for id {}.", circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/circuits")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Circuit> createCircuit(
            @RequestBody Circuit circuit) {
        circuit = circuitService.createCircuit(circuit);
        return new ResponseEntity<>(circuit, HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

    @PostMapping("/circuits/{circuitId}/legs")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Circuit> addCircuitLeg(
            @PathVariable(name = "circuitId") Long circuitId,
            @RequestBody CircuitLegRequest circuitLeg) {
        try {            
            Optional<Circuit> circuit = circuitService.load(circuitId);
            return circuit
                    .map(c ->
                            ResponseEntity.ok(circuitService.createLeg(c, circuitLeg)))
                    .orElse(ResponseEntity.badRequest().build());
        } catch (Exception ex) {
            log.error("Unable to modify circuit for id {} ", circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }

    }

    @DeleteMapping("/circuits/{circuitId}/legs/{circuitLegId}")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN)
    public ResponseEntity<Circuit> removeCircuitLeg(
            @PathVariable(name = "circuitId") Long circuitId,
            @PathVariable(name = "circuitLegId") Long circuitLegId) {
        try {
            Optional<Circuit> circuit = circuitService.load(circuitId);
            if (circuit.isPresent()) {
                Circuit updated = circuitService.removeLeg(circuit.get(), circuitLegId);
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            log.error("Unable to remove leg {} from circuit {}.", circuitLegId, circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/circuits/{circuitId}/legs/{circuitLegId}/update")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN)
    public ResponseEntity<Circuit> updateCircuitLeg(
            @PathVariable(name = "circuitId") Long circuitId,
            @PathVariable(name = "circuitLegId") Long circuitLegId,
            @RequestBody Map<String, Object> updateData) {
        try {
            log.info("Updating circuit leg {} for circuit {}", circuitLegId, circuitId);
            log.info("Circuit leg data: {}", updateData);
            Optional<Circuit> circuit = circuitService.load(circuitId);
            if (circuit.isPresent()) {
                Circuit updated = circuitService.updateLeg(
                    circuit.get(), circuitLegId, updateData);
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            log.error("Unable to update leg {} from circuit {}.", circuitLegId, circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/circuits/{circuitId}/legs/{circuitLegId}/addEntity")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN)
    public ResponseEntity<Circuit> addEntityToCircuitLeg(
            @PathVariable(name = "circuitId") Long circuitId,
            @PathVariable(name = "circuitLegId") Long circuitLegId,
            @RequestBody Map<String, Object> entityData) {  
        try {
            log.info("Adding entity to circuit leg {} for circuit {}", circuitLegId, circuitId);
            log.info("Entity data: {}", entityData);    
            Optional<Circuit> circuit = circuitService.load(circuitId);
            if (circuit.isPresent()) {
                CircuitLegEntity entity = new CircuitLegEntity();   
                /*{
                entityId=3_55bdf1c7-5941-11ef-be7b-bc24112ec32e_615c0eea-d281-11ef-9e80-bc2411305479,
                entityType=competition,
                entityNames=[Nuffle Spitfire Trophy, Spitfire Cup XXVII],
                game=BB3,
                platform=PC,
                ruleset=BB2020,
                isArchived=false
                }              
                */
                String game = (String)entityData.get("game");
                String eType = (String)entityData.get("legType");
                if (eType == null)
                    eType = (String)entityData.get("entityType");
                String platform = (String)entityData.get("platform");
                String ruleset = (String)entityData.get("ruleset");
                String ladderOpt = (String)entityData.get("ladderOption");
                String entityIdString = (String)entityData.get("entityId");
                entity.setLegType(eType != null && !eType.isEmpty()
                    ? EnumUtils.valueOfIgnoreCase(EntityType.class, eType)
                    : null);
                entity.setPlatform(platform != null && !platform.isEmpty()
                    ? EnumUtils.valueOfIgnoreCase(Platform.class, platform)
                    : null);
                entity.setGame(game != null && !game.isEmpty()
                    ? EnumUtils.valueOfIgnoreCase(GameType.class, game)
                    : null);
                Object eNames = entityData.get("entityNames");
                if (eNames != null) {
                    log.info(eNames.getClass().getName());
                    log.info(eNames.toString());                    
                    entity.setEntityNames(((List<String>)eNames).toArray(new String[0]));
                }
                entity.setRuleset(ruleset);
                entity.setLadderOption(ladderOpt != null && !ladderOpt.isEmpty() 
                    ? EnumUtils.valueOfIgnoreCase(LadderOption.class, ladderOpt)
                    : null);
               
                if (entityIdString != null && !entityIdString.isEmpty()) {
                    entity.setEntityId(IdentityUtil.fromId(entityIdString));
                } else {
                    String leagueIdString = (String)entityData.get("leagueId");
                    if (leagueIdString != null && !leagueIdString.isEmpty()) {
                        String competitionIdString = (String)entityData.get("competitionId");
                        Identity entityId;
                        int opus;
                        switch (entity.getGame()) {
                            case BB1: opus = 1; break;
                            case BB2: opus = 2; break;
                            case BB3: opus = 3; break;
                            default:
                                log.error("Unsupported game type: {}", entity.getGame());
                                return ResponseEntity.badRequest().build();
                        }
                        if (competitionIdString != null && !competitionIdString.isEmpty()) {
                            entityId = new CompositeIdentity(opus, new String[] {leagueIdString, competitionIdString} );
                        } else {
                            entityId = new SimpleIdentity(leagueIdString, opus);
                        }
                        entity.setEntityId(entityId);
                    } else {
                        log.error("No entityId or leagueId provided in entity data: {}", entityData);
                        return ResponseEntity.badRequest().build();
                    }
                }
                //entity.setExcludes((List<String>) entityData.get("excludes"));
                //entity.setIncludes((List<String>) entityData.get("includes"));
                
                Circuit updated = circuitService.addEntityToCircuitLeg(
                    circuit.get(), circuitLegId, entity);
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();   
            }
        } 
        catch (Exception ex) {
            log.error("Unable to add entity to leg {} from circuit {}.", circuitLegId, circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
