package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.cyanide.api.requests.LookupRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.LookupResponse;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LookupController {
    private final CyanideApiService cyanideApiService;

    @PostMapping("/lookup")
    public ResponseEntity<LookupResponse> doLookupTeams(@RequestBody LookupRequest lookupRequest) {
        try {
            LookupResponse lookup = cyanideApiService.lookup(lookupRequest);
            return ResponseEntity.ok(lookup);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
