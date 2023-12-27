package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupResponse;
import de.dbbcev.dbbcbb3facade.domain.CoachRepository;
import de.dbbcev.dbbcbb3facade.domain.LeagueCollectionRepository;
import de.dbbcev.dbbcbb3facade.domain.TeamRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Coach;
import de.dbbcev.dbbcbb3facade.domain.model.LeagueCollection;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TeamsController {

    private final CyanideApiService cyanideApiService;

    private final TeamRepository teamRepository;

    private final LeagueCollectionRepository leagueCollectionRepository;

    @GetMapping("/teams/{leagueId}")
    public ResponseEntity<List<TeamsResponse>> getTeams(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Team> teams = teamRepository.findByLeagueId(leagueId);
            List<TeamsResponse> teamsResponses = teams
                    .stream()
                    .map(this::toTeamsResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(teamsResponses);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
    private TeamsResponse toTeamsResponse(Team team) {
        TeamsResponse teamsResponse = new TeamsResponse();
        teamsResponse.setTeam(team);
        teamsResponse.setCoach(extractCoach(team));
        return teamsResponse;
    }

    @GetMapping("/team/{teamUuid}")
    public ResponseEntity<TeamsResponse> getTeam(@PathVariable(name = "teamUuid") UUID teamUuid) {
        Optional<Team> team = teamRepository.findById(teamUuid);
        return team.map(t -> ResponseEntity.ok(toTeamsResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Coach extractCoach(Team team) {
        Coach coach = new Coach();
        // coach.setId(team.getCoachId());
        coach.setName(team.getCoachName());
        return coach;
    }
}
