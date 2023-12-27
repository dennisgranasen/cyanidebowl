package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.domain.StatusRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Status;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
