package net.warp_scores.warpscores.controller;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LookupController {
    private final CyanideApiService cyanideApiService;

    @PostMapping("/lookup")
    public ResponseEntity<LookupResponse> performLookup(@RequestBody LookupRequest lookupRequest) {
        try {
            LookupResponse lookup = cyanideApiService.lookup(lookupRequest);
            return ResponseEntity.ok(lookup);
        } catch (Exception ex) {
            log.error("Unable to perform lookup {}.", lookupRequest, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
