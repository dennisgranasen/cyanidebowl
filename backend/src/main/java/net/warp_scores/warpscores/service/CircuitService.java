package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.model.CircuitLegEntity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LadderOption;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.requests.CircuitLegRequest;
import net.warp_scores.warpscores.utils.EnumUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitService {
    public static final long DUMMY_CIRCUIT_ID = -42L;

    private final CircuitRepository circuitRepository;
    private final DataCollectionRepository collectionRepository;
    private final SequenceGenerator sequenceGenerator;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional(readOnly = true)
    @DurationLogging
    public Optional<Circuit> load(Long circuitId) {
        return circuitRepository.findById(circuitId);
    }

    @Transactional(readOnly = true)
    @DurationLogging
    public List<Circuit> loadAll() {
        List<Circuit> circuits = circuitRepository.findAll();
        return circuits;
    }

    /*
    private CircuitLeg createDummyCircuitLeg(League league) {
        CircuitLeg circuitLeg = new CircuitLeg();
        circuitLeg.setLegType(CircuitLegType.League);
        circuitLeg.setGame(GameType.BB3);
        circuitLeg.setPlatform(Platform.CROSS);
        circuitLeg.setIsKnockout(false);
        circuitLeg.setIsCollected(true);
        circuitLeg.setLabel(league.getName());
        circuitLeg.setCompetitionId(league.getLeagueId()); // Use Identity-based id
        return circuitLeg;
    }
    */

    public Circuit createCircuit(Circuit circuit) {
        return newCircuit(circuit);
    }

    private Circuit newCircuit(Circuit circuit) {
        circuit.setCircuitId(sequenceGenerator.nextIdFor(Circuit.class));
        log.info("Created circuit {}", circuit);
        return circuitRepository.save(circuit);
    }

    public Circuit createLeg(Circuit circuit, CircuitLegRequest req) {
        log.info("Adding leg {} to circuit {}.", req, circuit);
        CircuitLeg newLeg = new CircuitLeg();
        newLeg.setCircuitLegId(sequenceGenerator.nextIdFor(CircuitLeg.class));

        // --- NEW: Build entities list ---
        Collection<CircuitLegEntity> entities = req.getEntities();
        if (entities != null)
            newLeg.setEntities(entities.stream().toList());
        else {          
            Identity entityId = IdentityUtil.fromId(req.getEntityId());  
            newLeg.setEntities(new ArrayList<>());
            newLeg.getEntities().add(
                new CircuitLegEntity(){
                    {                        
                        setEntityId(entityId);
                        setLegType(entityId instanceof CompositeIdentity
                            ? EntityType.Competition : EntityType.League);
                        setGame(
                            req.getGame() != null && !req.getGame().isEmpty()
                                ? EnumUtils.valueOfIgnoreCase(GameType.class, req.getGame())
                                : null
                        );
                        setPlatform(
                            req.getPlatform() != null && !req.getPlatform().isEmpty()
                                ? EnumUtils.valueOfIgnoreCase(Platform.class, req.getPlatform())
                                : null
                        );
                        setRuleset(
                            req.getRuleset() != null && !req.getRuleset().isEmpty()
                                ? req.getRuleset()
                                : null
                        );
                        setLadderOption(
                            req.getLadderOption() != null && !req.getLadderOption().isEmpty()
                                ? EnumUtils.valueOfIgnoreCase(LadderOption.class, req.getLadderOption())
                                : null
                        );
                    }
                });
        }

        newLeg.setLabel(req.getLabel());
        newLeg.setIsCollected(req.getIsCollected() == null ? true : Boolean.parseBoolean(req.getIsCollected()));

        circuit.addLeg(newLeg);

        // Optionally: updateLeagueCollection for each entity
        for (CircuitLegEntity entity : entities) {
            if (entity.getEntityId() != null && newLeg.getIsCollected() != null && newLeg.getIsCollected()) {
                updateLeagueCollection(entity.getEntityId(), entity.getLegType(), true);
            }
        }

        return circuitRepository.save(circuit);
    }

    public Circuit updateLeg(Circuit circuit, long circuitLegId, Map<String, Object> circuitLeg) {
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null)
            throw new IllegalArgumentException("Circuit leg with ID " + circuitLegId + " not found in circuit.");

        boolean wasCollected = leg.getIsCollected() != null && leg.getIsCollected();

        // --- Update label, game, platform, ruleset, etc as before ---
        if (circuitLeg.containsKey("label"))
            leg.setLabel(circuitLeg.get("label") instanceof String
                    ? (String) circuitLeg.get("label")
                    : String.valueOf(circuitLeg.get("label")));
        if (circuitLeg.containsKey("isCollected")) 
            leg.setIsCollected(circuitLeg.get("isCollected") instanceof Boolean
                    ? (Boolean) circuitLeg.get("isCollected")
                    : Boolean.parseBoolean((String) circuitLeg.get("isCollected")));

        // --- NEW: Update entities if present ---
        if (circuitLeg.containsKey("entities")) {
            @SuppressWarnings("unchecked")
            List<CircuitLegEntity> newEntities = (List<CircuitLegEntity>) circuitLeg.get("entities");
            // Optionally: handle collection updates for removed/added entities
            if (wasCollected != leg.getIsCollected()) {
                // Remove collection for old entities if needed
                for (CircuitLegEntity entity : leg.getEntities()) {
                    updateLeagueCollection(entity.getEntityId(), entity.getLegType(), leg.getIsCollected());
                }
            }
            leg.setEntities(newEntities);
        }

        // If collection status changed, update for all entities
        if (wasCollected != leg.getIsCollected()) {
            for (CircuitLegEntity entity : leg.getEntities()) {
                updateLeagueCollection(entity.getEntityId(), entity.getLegType(), leg.getIsCollected());
            }
        }

        return circuitRepository.save(circuit);
    }

    public Circuit removeLeg(Circuit circuit, Long circuitLegId) {
        log.info("Removing leg {} from circuit {}.", circuitLegId, circuit.getCircuitId());
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null) {
            log.warn("Circuit leg with ID {} not found in circuit {}.", circuitLegId, circuit.getCircuitId());
            return circuit; // No changes made
        }
        // --- NEW: Remove collection for all entities in the leg ---
        if (leg.getEntities() != null && leg.getIsCollected() != null && leg.getIsCollected()) {
            for (CircuitLegEntity entity : leg.getEntities()) {
                updateLeagueCollection(entity.getEntityId(), entity.getLegType(), false);
            }
        }
        // Remove the leg from the circuit
        circuit.getCircuitLegs().removeIf(l -> circuitLegId.equals(l.getCircuitLegId()));
        return circuitRepository.save(circuit);
    }

    private void updateLeagueCollection(Identity leagueId, EntityType legType, boolean toBeCollected) {
        if (!toBeCollected) {
            if (collectionRepository.existsById(leagueId))
                collectionRepository.deleteById(leagueId);
            else
                log.warn("No collection found for identity {} to delete.", leagueId);        
        } else {
            DataCollection lc = new DataCollection(leagueId, legType);
            collectionRepository.save(lc);
        }
    }

    public Circuit addEntityToCircuitLeg(
            Circuit circuit, long circuitLegId, CircuitLegEntity entity) {
        log.info("Adding entity {} to circuit leg {} for circuit {}.", entity, circuitLegId, circuit.getCircuitId());
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null) {
            log.warn("Circuit leg with ID {} not found in circuit {}.", circuitLegId, circuit.getCircuitId());
            return circuit; // No changes made
        }
        //leg.addEntity(entity);
        leg.getEntities().add(entity);
        if (leg.getIsCollected() != null && leg.getIsCollected()) {
            updateLeagueCollection(entity.getEntityId(), entity.getLegType(), true);        
        }
        return circuitRepository.save(circuit);
    }
}
