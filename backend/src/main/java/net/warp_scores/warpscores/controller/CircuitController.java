package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.service.CircuitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

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
    public ResponseEntity<Circuit> getCircuit(@PathVariable(name = "circuitId") Integer circuitId) {
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
    public ResponseEntity<Circuit> createCircuit(@RequestBody Circuit circuit) {
        circuit = circuitService.createCircuit(circuit);
        return new ResponseEntity(circuit, HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

    @PostMapping("/circuits/{circuitId}/legs")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Circuit> addCircuitLeg(@PathVariable(name = "circuitId") Integer circuitId,
            @RequestBody CircuitLeg circuitLeg) {
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

}
