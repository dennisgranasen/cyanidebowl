package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.model.Status;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.service.CyanideApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class StatusController {
    private final StatusRepository statusRepository;

    @GetMapping("/status")
    public ResponseEntity<Status> getStatus() {
        Optional<Status> byId = statusRepository.findById(CyanideApiService.BB3_GAME_NAME);
        return byId
                .map(status -> ResponseEntity.ok().body(status))
                .orElse(ResponseEntity.internalServerError().build());
    }
}
