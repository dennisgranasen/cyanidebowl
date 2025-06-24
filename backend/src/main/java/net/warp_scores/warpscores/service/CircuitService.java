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
    private final LeagueService leagueService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

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
                .filter(cl -> cl.getLegType() == EntityType.League)
                .map(CircuitLeg::getEntityId)
                .collect(Collectors.toSet());
        // Use getLeagueId() or getIdentity().getId() depending on your League model
        return leagues
                .stream()
                .filter(league -> !leagueIdsInCircuits.contains(league.getId()))
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

    public Circuit createLeg(Circuit circuit, CircuitLegRequest req) {
        log.info("Adding leg {} to circuit {}.", req, circuit);
        CircuitLeg newLeg = new CircuitLeg();
        newLeg.setCircuitLegId(sequenceGenerator.nextIdFor(CircuitLeg.class));
        Integer opus = null;
        String gameStr = req.getGame();
        if (gameStr != null && gameStr.substring(0,2).equalsIgnoreCase("bb")) {
            opus = Integer.parseInt(gameStr.substring(2));
        } else {
            log.warn("Game type is not specified in the request, using default opus.");
            opus = defaultOpus; // Default opus, adjust as necessary
        }

        Identity entityId;
        if (req.getCompetitionId() != null) {
            entityId = new CompositeIdentity(opus, req.getLeagueId(), req.getCompetitionId());
        } else {
            entityId = new SimpleIdentity(req.getLeagueId(), opus);
        }
        newLeg.setEntityId(entityId);
        newLeg.setLegType(
            EnumUtils.valueOfIgnoreCase(EntityType.class,
                req.getLegType()));
        newLeg.setLabel(req.getLabel());
        newLeg.setGame(
            EnumUtils.valueOfIgnoreCase(GameType.class, 
                gameStr));
        newLeg.setPlatform(
            EnumUtils.valueOfIgnoreCase(Platform.class, 
                req.getPlatform()));
        newLeg.setRuleset(req.getRuleset());
        if (req.getIsCollected() == null) {
            newLeg.setIsCollected(true); // Default to true if not specified
        } else {
            newLeg.setIsCollected(Boolean.parseBoolean(req.getIsCollected()));
        }
        if (req.getIsArchived() == null) {
            newLeg.setIsArchived(false); // Default to false if not specified
        } else {
            newLeg.setIsArchived(Boolean.parseBoolean(req.getIsArchived()));
        }
        String ladderOptionStr = req.getLadderOption();
        if (ladderOptionStr == null || ladderOptionStr.isEmpty()) 
            newLeg.setLadderOption(LadderOption.None); // Default to NONE if not specified
        else
            newLeg.setLadderOption(
                EnumUtils.valueOfIgnoreCase(LadderOption.class, 
                    ladderOptionStr.replace("-","")));
        circuit.addLeg(newLeg);
        if (newLeg.getEntityId() != null && newLeg.getIsCollected() != null && newLeg.getIsCollected()) {
            updateLeagueCollection(
                newLeg.getEntityId(), newLeg.getLegType(), true);
        }
        return circuitRepository.save(circuit);
    }

    public Circuit updateLeg(Circuit circuit, long circuitLegId, Map<String, Object> circuitLeg) {
        //circuitLeg.setCircuitLegId(sequenceGenerator.nextIdFor(CircuitLeg.class));
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null)
            throw new IllegalArgumentException("Circuit leg with ID " + circuitLegId + " not found in circuit.");
        boolean wasCollected = leg.getIsCollected() != null && leg.getIsCollected();
        if (circuitLeg.containsKey("entityId")) {
            Identity id = IdentityUtil.fromId((String)circuitLeg.get("entityId"));
            if (id != null)
                leg.setEntityId(id);
        }
        if (circuitLeg.containsKey("legType"))
            leg.setLegType(circuitLeg.get("legType") instanceof EntityType
                    ? (EntityType) circuitLeg.get("legType")
                    : EntityType.valueOf((String) circuitLeg.get("legType")));
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
        if (leg.getEntityId() != null && wasCollected != leg.getIsCollected())
            updateLeagueCollection(
                leg.getEntityId(), leg.getLegType(), leg.getIsCollected());
        return circuitRepository.save(circuit);
    }

    public Circuit removeLeg(Circuit circuit, Long circuitLegId) {
        log.info("Removing leg {} from circuit {}.", circuitLegId, circuit.getCircuitId());
        CircuitLeg leg = circuit.getCircuitLeg(circuitLegId);
        if (leg == null) {
            log.warn("Circuit leg with ID {} not found in circuit {}.", circuitLegId, circuit.getCircuitId());
            return circuit; // No changes made
        }
        if (leg.getEntityId() != null && leg.getIsCollected() != null && leg.getIsCollected()) {
            log.info("Leg {} in circuit {} is collected, removing collection.", circuitLegId, circuit.getCircuitId());
            updateLeagueCollection(leg.getEntityId(), leg.getLegType(), false);
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
}
