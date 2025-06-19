package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.model.CircuitLegType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LadderOption;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.LeagueCollection;
import net.warp_scores.warpscores.model.Platform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LeagueCollectionRepository collectionRepository;
    private final SequenceGenerator sequenceGenerator;
    private final LeagueService leagueService;

    @Transactional(readOnly = true)
    @DurationLogging
    public Optional<Circuit> load(Long circuitId) {
        if (DUMMY_CIRCUIT_ID == circuitId) {
            return createDummyCircuitIfNecessary(circuitRepository.findAll());
        } else {
            return circuitRepository.findById(circuitId);
        }
    }

    @Transactional(readOnly = true)
    @DurationLogging
    public List<Circuit> loadAll() {
        List<Circuit> circuits = circuitRepository.findAll();
        addDummyCircuitForLegacyLeagues(circuits);
        return circuits;
    }

    @Deprecated
    private void addDummyCircuitForLegacyLeagues(List<Circuit> circuits) {
        Optional<Circuit> circuit = createDummyCircuitIfNecessary(circuits);
        circuit.ifPresentOrElse(circuits::add,
                () -> log.info("No legacy leagues found. Code can be cleaned up."));
    }

    @Deprecated
    private Optional<Circuit> createDummyCircuitIfNecessary(List<Circuit> circuits) {
        List<League> leaguesWithoutCircuits = getLeaguesWithoutCircuits(circuits);
        return createDummyCircuit(leaguesWithoutCircuits);
    }

    @Deprecated
    private List<League> getLeaguesWithoutCircuits(List<Circuit> circuits) {
        List<League> leagues = leagueService.loadAll();
        Set<Identity> leagueIdsInCircuits = circuits
                .stream()
                .flatMap(c -> c.getCircuitLegs().stream())
                .filter(cl -> cl.getLegType() == CircuitLegType.League)
                .map(CircuitLeg::getEntityId)
                .collect(Collectors.toSet());
        // Use getLeagueId() or getIdentity().getId() depending on your League model
        return leagues
                .stream()
                .filter(league -> !leagueIdsInCircuits.contains(league.getIdentity()))
                .collect(Collectors.toList());
    }

    private Optional<Circuit> createDummyCircuit(List<League> leaguesWithoutCircuits) {
        if (leaguesWithoutCircuits.isEmpty()) {
            return Optional.empty();
        }
        Circuit circuit = new Circuit();
        circuit.setCircuitId(DUMMY_CIRCUIT_ID);
        circuit.setCircuitName("Leagues without circuits");
        // If you want to add dummy legs, uncomment and update the following:
        // circuit.setCircuitLegs(leaguesWithoutCircuits.stream().map(this::createDummyCircuitLeg).toList());
        return Optional.of(circuit);
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

    public Circuit createLeg(Circuit circuit, CircuitLeg circuitLeg) {
        log.info("Adding leg {} to circuit {}.", circuitLeg, circuit);        
        circuitLeg.setCircuitLegId(sequenceGenerator.nextIdFor(CircuitLeg.class));
        circuit.addLeg(circuitLeg);
        return circuitRepository.save(circuit);
    }

    public Circuit updateLeg(Circuit circuit, long circuitLegId, Map<String, Object> circuitLeg) {
        //circuitLeg.setCircuitLegId(sequenceGenerator.nextIdFor(CircuitLeg.class));
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null)
            throw new IllegalArgumentException("Circuit leg with ID " + circuitLegId + " not found in circuit.");
        boolean wasCollected = leg.getIsCollected();
        if (circuitLeg.containsKey("entityId")) {
            Identity id = IdentityUtil.fromId((String)circuitLeg.get("entityId"));
            if (id != null)
                leg.setEntityId(id);
        }
        if (circuitLeg.containsKey("legType"))
            leg.setLegType(circuitLeg.get("legType") instanceof CircuitLegType
                    ? (CircuitLegType) circuitLeg.get("legType")
                    : CircuitLegType.valueOf((String) circuitLeg.get("legType")));
        if (circuitLeg.containsKey("label"))
            leg.setLabel(circuitLeg.get("label") instanceof String
                    ? (String) circuitLeg.get("label")
                    : String.valueOf(circuitLeg.get("label")));
        if (circuitLeg.containsKey("game")) 
            leg.setGame(circuitLeg.get("game") instanceof GameType
                    ? (GameType) circuitLeg.get("game")
                    : GameType.valueOf((String) circuitLeg.get("game")));
        if (circuitLeg.containsKey("platform")) 
            leg.setPlatform(circuitLeg.get("platform") instanceof Platform
                    ? (Platform) circuitLeg.get("platform")
                    : Platform.valueOf((String) circuitLeg.get("platform")));
        if (circuitLeg.containsKey("ruleset")) 
            leg.setRuleset(circuitLeg.get("ruleset") instanceof String
                    ? (String) circuitLeg.get("ruleset")
                    : String.valueOf(circuitLeg.get("ruleset")));
        if (circuitLeg.containsKey("isCollected")) 
            leg.setIsCollected(circuitLeg.get("isCollected") instanceof Boolean
                    ? (Boolean) circuitLeg.get("isCollected")
                    : Boolean.parseBoolean((String) circuitLeg.get("isCollected")));
        if (circuitLeg.containsKey("isArchived"))
            leg.setIsArchived(circuitLeg.get("isArchived") instanceof Boolean
                    ? (Boolean) circuitLeg.get("isArchived")
                    : Boolean.parseBoolean((String) circuitLeg.get("isArchived")));
        if (circuitLeg.containsKey("ladderOption")) 
            leg.setLadderOption(circuitLeg.get("ladderOption") instanceof LadderOption
                    ? (LadderOption) circuitLeg.get("ladderOption")
                    : LadderOption.valueOf((String)circuitLeg.get("ladderOption")));

        //log.info("Updated leg {} in circuit {}.", circuitLegId, circuit.getCircuitId());
        if (wasCollected && !leg.getIsCollected()) { 
            log.info("Leg {} in circuit {} was collected, but is now removed from collection.", circuitLegId, circuit.getCircuitId());
            LeagueCollection lc = new LeagueCollection(leg.getEntityId());
            collectionRepository.insert(lc);
        } else if (!wasCollected && leg.getIsCollected()) {
            log.info("Leg {} in circuit {} was not collected, but collection is now started.", circuitLegId, circuit.getCircuitId());
            collectionRepository.deleteById(leg.getEntityId());
        }  
        return circuitRepository.save(circuit);
    }

    public Circuit removeLeg(Circuit circuit, Long circuitLegId) {
        log.info("Removing leg {} from circuit {}.", circuitLegId, circuit.getCircuitId());
        circuit.getCircuitLegs().removeIf(leg -> circuitLegId.equals(leg.getCircuitLegId()));
        return circuitRepository.save(circuit);
    }
}
