package net.warp_scores.warpscores.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.service.UUIDConverter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CircuitController {
    
    private final CircuitRepository circuitRepository;

    @GetMapping("/circuits")
    public ResponseEntity<List<Circuit>> getCircuits() {
        try {
            List<Circuit> circuits = circuitRepository.findAll();
            return ResponseEntity.ok(circuits);
        } catch (Exception ex) {
            log.error("Unable to retrieve circuits", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/circuit/{circuitId}")
    public ResponseEntity<Circuit> geCircuit(@PathVariable(name = "circuitId") Integer circuitId) {
        try {
            Circuit circuit = circuitRepository.findById(circuitId).orElseThrow();
            return ResponseEntity.ok(circuit);
        } catch (Exception ex) {
            log.error("Unable to retrieve circuit " + circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/circuit/{circuitName}/new")
    @Transactional
    public void createCircuit(@PathVariable(name = "circuitName") String circuitName) {
        var c = newCircuit(circuitName);
        log.info("Added circuit {}.", c);
        circuitRepository.insert(c);
    }


    @PostMapping("/circuit/{circuitId}/addLeg")
    @Transactional
    public ResponseEntity<Circuit> addCircuitLeg(@PathVariable(name = "circuitId") Integer circuitId,
                                       @RequestBody CircuitLeg circuitLeg) {
        try {
            Circuit circuit = circuitRepository.findById(circuitId).orElseThrow();
            log.info("adding cc: {}", circuitLeg);
            Optional<Integer> maxId = circuit.getCircuitLegs()
                .stream()
                .map((cc) -> cc.getCircuitLegId())
                .max(Integer::compare);
            circuitLeg.setCircuitLegId(maxId.orElse(0) + 1);
            circuit.addLeg(circuitLeg);
            Circuit c2 = circuitRepository.save(circuit);

            return ResponseEntity.ok(c2);
/*
            List<Competition> competitions = competitionService.loadForLeagueAndStatuses(leagueId, competitionStatuses);
            competitions = competitions
                    .stream()
                    .filter(competitionService::competitionConsideredActive)
                    .sorted()
                    .collect(Collectors.toUnmodifiableList())                   ;
            return ResponseEntity.ok(competitions);
            */
        } catch (Exception ex) {
            log.error("Unable to modify circuit for id {} ", circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }

    }

    @SuppressWarnings("unchecked")
    private Circuit newCircuit(String name) {
        Circuit circuit = new Circuit();
        var maxId = circuitRepository.findAll().stream().mapToInt(
            new ToIntFunction<Circuit>() {
                @Override
                public int applyAsInt(Circuit lc) {
                    return lc.getCircuitId();
                }
            }).max().orElse(0);
        circuit.setCircuitId(maxId + 1);
        circuit.setCircuitName(name);
        circuit.setCircuitLegs(Collections.EMPTY_LIST);
        log.info("Created circuit {}", circuit);
        return circuit;
    }
}
