package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.model.CircuitLegType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Platform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitService {
    public static final long DUMMY_CIRCUIT_ID = -42L;

    private final CircuitRepository circuitRepository;
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
                .filter(league -> !leagueIdsInCircuits.contains(league.getLeagueId()))
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

    public Circuit removeLeg(Circuit circuit, Long circuitLegId) {
        log.info("Removing leg {} from circuit {}.", circuitLegId, circuit.getCircuitId());
        circuit.getCircuitLegs().removeIf(leg -> circuitLegId.equals(leg.getCircuitLegId()));
        return circuitRepository.save(circuit);
    }
}
