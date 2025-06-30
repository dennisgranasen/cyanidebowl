package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_REGISTER_LEAGUE;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DataCollectionController {

    private final DataCollectionRepository dataCollectionRepository;

    private final CyanideApiService cyanideApiService;


    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @PostMapping("/leagueCollection/{leagueId}")
    @PreAuthorize(AUTHORITY_WRITE_REGISTER_LEAGUE) // ✨
    public ResponseEntity<List<League>> createLeagueCollection(
        @PathVariable(name = "leagueId") String leagueId,
        @RequestParam(name = "opus", required = false) Integer opus
    ) {
        Identity identity = new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus));
        List<League> leagues = doCreateLeagueCollection(identity);
        return ResponseEntity.ok(leagues);
    }

    private List<League> doCreateLeagueCollection(Identity id) {
        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setLeague_id(id.getValue());
        int opus = id.getOpus();
        lookupRequest.setOpus(opus);
        LookupResponse lookup = cyanideApiService.lookup(lookupRequest);
        List<DataCollection> leagueCollections = Arrays.stream(lookup.getLeagues())
                .map((idWithName) -> newLeagueCollection(
                    new SimpleIdentity(idWithName.getId(), opus)/*, idWithName.getName()*/))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        leagueCollections = dataCollectionRepository.saveAll(leagueCollections);
        log.info("Added league collections {}.", leagueCollections);

        return leagueCollections
                .stream()
                .map(lc -> cyanideApiService.loadLeague(lc.getId()))
                .toList();
    }

    private DataCollection newLeagueCollection(Identity id/* , String name*/) {
        DataCollection leagueCollection = new DataCollection(id, EntityType.League);
        //leagueCollection.setCollectionActive(true);
        //leagueCollection.setName(name); // not used in the UI, but can be useful for debugging
        //leagueCollection.setLeagueName(idWithName.getName());
        return leagueCollection;
    }
}
