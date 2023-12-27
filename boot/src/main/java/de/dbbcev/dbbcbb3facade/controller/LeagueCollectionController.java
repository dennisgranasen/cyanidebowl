package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupResponse;
import de.dbbcev.dbbcbb3facade.domain.LeagueCollectionRepository;
import de.dbbcev.dbbcbb3facade.domain.model.LeagueCollection;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/leagueCollection")
    public void createLeagueCollection(@RequestBody UUID leagueId) {
        createLeagueCollection(leagueId);
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
        leagueCollection.setLeagueId(idWithName.getIdAsUUIDOrNull().get());
        return leagueCollection;
    }
}
