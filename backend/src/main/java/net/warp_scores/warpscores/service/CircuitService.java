package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitService {
    private final CircuitRepository circuitRepository;
    private final SequenceGenerator sequenceGenerator;

    @Transactional(readOnly = true)
    public Optional<Circuit> load(Integer circuitId) {
        return circuitRepository.findById(circuitId);
    }

    @Transactional(readOnly = true)
    public List<Circuit> loadAll() {
        return circuitRepository.findAll();
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
