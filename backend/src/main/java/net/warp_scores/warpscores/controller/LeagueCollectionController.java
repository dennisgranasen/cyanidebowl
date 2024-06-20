package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.model.LeagueCollection;
import net.warp_scores.warpscores.service.CyanideApiService;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LeagueCollectionController {

    private final LeagueCollectionRepository leagueCollectionRepository;

    private final CyanideApiService cyanideApiService;

    private final UUIDConverter uuidConverter;

    @PostMapping("/leagueCollection/{leagueId}")
    public void createLeagueCollection(@PathVariable(name = "leagueId") UUID leagueId) {
        doCreateLeagueCollection(leagueId);
    }

    private void doCreateLeagueCollection(UUID leagueId) {
        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setLeague_id(leagueId);
        LookupResponse lookup = cyanideApiService.lookup(lookupRequest);
        Set<LeagueCollection> leagueCollections = Arrays.stream(lookup.getLeagues())
                .map(this::newLeagueCollection)
                .collect(Collectors.toSet());
        log.info("Added league collections {}.", leagueCollections);
        leagueCollectionRepository.saveAll(leagueCollections);
    }

    private LeagueCollection newLeagueCollection(IdWithName idWithName) {
        LeagueCollection leagueCollection = new LeagueCollection();
        leagueCollection.setCollectionActive(true);
        leagueCollection.setLeagueName(idWithName.getName());
        leagueCollection.setLeagueId(uuidConverter.toUuid(idWithName.getId()).get());
        return leagueCollection;
    }
}
