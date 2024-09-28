package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
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
import java.util.UUID;
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
    public Optional<Circuit> load(Long circuitId) {
        if (DUMMY_CIRCUIT_ID == circuitId) {
            return createDummyCircuitIfNecessary(circuitRepository.findAll());
        } else {
            return circuitRepository.findById(circuitId);
        }
    }

    @Transactional(readOnly = true)
    public List<Circuit> loadAll() {
        List<Circuit> circuits = circuitRepository.findAll();

        addDummyCircuitForLegacyLeagues(circuits);

        return circuits;
    }

    @Deprecated
    private void addDummyCircuitForLegacyLeagues(List<Circuit> circuits) {
        Optional<Circuit> circuit = createDummyCircuitIfNecessary(circuits);
        circuit.ifPresentOrElse(c -> circuits.add(c),
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
        List<UUID> leagueUuidsInCircuits = circuits
                .stream()
                .flatMap(c -> c.getCircuitLegs().stream())
                .collect(Collectors.toSet())
                .stream()
                .filter(cl -> cl.getLegType() == CircuitLegType.League)
                .map(CircuitLeg::getCompetitionId)
                .map(UUID::fromString)
                .toList();
        List<League> leaguesWithoutCircuits = leagues
                .stream()
                .filter(league -> !leagueUuidsInCircuits.contains(league.getUuid()))
                .toList();
        return leaguesWithoutCircuits;
    }

    private Optional<Circuit> createDummyCircuit(List<League> leaguesWithoutCircuits) {
        if (leaguesWithoutCircuits.isEmpty()) {
            return Optional.empty();
        }
        Circuit circuit = new Circuit();
        circuit.setCircuitId(DUMMY_CIRCUIT_ID);
        circuit.setCircuitName("Leagues without circuits");
        circuit.setCircuitLegs(leaguesWithoutCircuits.stream().map(this::createDummyCircuitLeg).toList());
        return Optional.of(circuit);
    }

    private CircuitLeg createDummyCircuitLeg(League league) {
        CircuitLeg circuitLeg = new CircuitLeg();
        circuitLeg.setLegType(CircuitLegType.League);
        circuitLeg.setGame(GameType.BB3);
        circuitLeg.setPlatform(Platform.CROSS);
        circuitLeg.setIsKnockout(false);
        circuitLeg.setIsCollected(true);
        circuitLeg.setLabel(league.getName());
        circuitLeg.setCompetitionId(league.getUuid().toString());
        return circuitLeg;
    }

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
}
