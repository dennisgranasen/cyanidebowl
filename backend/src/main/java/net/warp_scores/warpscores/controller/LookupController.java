package net.warp_scores.warpscores.controller;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.responses.DetailedLookupResponse;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@Slf4j
public class LookupController {
    private final CyanideApiService cyanideApiService;

    @Value("${cyanide.default.opus:1}")
    private int defaultOpus; // Default opus value, can be overridden in application properties

    @PostMapping("/lookup")
    @PreAuthorize(Authorities.AUTHORITY_WRITE_REGISTER_LEAGUE)
    public ResponseEntity<LookupResponse> performLookup(@RequestBody LookupRequest lookupRequest) {

        log.info("Lookup request: {}", lookupRequest);
        try {
            // If search is a numeric league ID and opus is 1 or 2, do direct league lookup
            String leagueName = lookupRequest.getLeague_name();
            Integer opus = lookupRequest.getOpus();
            if (opus == null) {
                opus = defaultOpus; // Default opus if not provided
            }
            boolean isNumericLeagueId = leagueName != null && leagueName.matches("^\\d+$");
            log.info("League name: {}, opus: {}, isNumericLeagueId: {}", leagueName, opus, isNumericLeagueId);            
            if (isNumericLeagueId && (opus < 3)) {                
                Identity leagueIdentity = new SimpleIdentity(leagueName, opus);
                log.info("Performing direct league lookup for league ID: {}", leagueIdentity);
                League league = cyanideApiService.loadLeague(leagueIdentity);
                if (league == null) {
                    log.warn("League {} not found for opus {}", leagueName, opus);
                } else {
                    log.info("League found: {}", league);
                }
                log.info("Loading competitions for id: {}", leagueIdentity);
                List<Competition> competitions = cyanideApiService.loadCompetitions(leagueIdentity);
                if (competitions == null || competitions.isEmpty()) {
                    log.warn("No competitions found for league {}", leagueName);
                } else {
                    log.info("Competitions found: {}", competitions.size());
                }

                LookupResponse resp = new LookupResponse();
                if (league != null)
                    resp.setFullLeagues(new League[]{league});
                if (competitions != null && !competitions.isEmpty())
                    resp.setFullCompetitions(competitions.toArray(new Competition[0]));
                return ResponseEntity.ok(resp);
            }

            // Default: normal lookup
            LookupResponse lookup = cyanideApiService.lookup(lookupRequest);
            log.info("Lookup response: {}", lookup);
            if (lookupRequest.getIncludeDetails() && lookup != null) {
                return lookupDetails(lookup, lookupRequest);
            } else {
                return ResponseEntity.ok(lookup);
            }
        } catch (Exception ex) {
            log.error("Unable to perform lookup {}.", lookupRequest, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<LookupResponse> lookupDetails(LookupResponse lookup, LookupRequest lookupRequest) {

        if (lookup == null || (lookup.getLeagues() == null && lookup.getCompetitions() == null && lookup.getCoaches() == null)) {
            log.warn("Lookup response is null or league is null for request: {}", lookupRequest);
            return ResponseEntity.ok(lookup);
        }
        try {
            int opus = lookupRequest.getOpus();
            log.info("Opus: {}", opus);
            List<League> leagues = new ArrayList<>();
            List<Competition> competitions = new ArrayList<>();
            for (var leagueIdWithName : lookup.getLeagues()) {
                //log.info("Looking up details for league: {}", leagueIdWithName.getId());
                Identity leagueIdentity = new SimpleIdentity(leagueIdWithName.getId(), opus);
                League l = cyanideApiService.loadLeague(leagueIdentity);
                if (l == null) {
                    log.warn("League {} not found in database, skipping.", leagueIdWithName.getId());
                    continue;
                }
                leagues.add(l);
                List<Competition> cs = cyanideApiService.loadCompetitions(leagueIdentity);
                if (cs == null || cs.isEmpty()) {
                    log.warn("No competitions found for league {}", leagueIdWithName.getId());
                    continue;
                }
                competitions.addAll(cs);
                log.info("Found {} competitions for league {}", cs.size(), leagueIdWithName.getId());
            }
            DetailedLookupResponse detailedLookup = new DetailedLookupResponse(
                lookup,
                leagues.toArray(new League[0]),
                competitions.toArray(new Competition[0])
            );
            return ResponseEntity.ok(detailedLookup);
        } catch (Exception ex) {
            log.error("Error looking up details {}", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
