package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.TeamCollection;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamDomainService {


    @Autowired
    private final TeamRepository teamRepository;
    @Autowired
    private final TeamPopulator teamPopulator;
    @Autowired
    private final TeamCollectionDomainService competitionTeamsDomainService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @Transactional
    public List<Team> createOrUpdateTeams(TeamsResponse teamsResponse, int opus) {
        if (teamsResponse == null || teamsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        List<Team> teams = Arrays.stream(teamsResponse.getTeams())
                .map((apiTeam) -> internalCreateOrUpdateTeam(apiTeam, opus))
                .collect(Collectors.toList());
        return teamRepository.saveAll(teams);
    }

    @Transactional
    public Team createOrUpdateTeam(TeamResponse teamResponse, int opus) {
        if (teamResponse == null || teamResponse.isEmpty()) {
            return null;
        }
        Team team = internalCreateOrUpdateTeam(
            teamResponse.getTeam(), 
            teamResponse.getRoster(),
            opus);
        return teamRepository.save(team);
    }

    @Transactional
    public List<Team> findByCompetitionId(Identity competitionId) {
        Optional<TeamCollection> competitionTeams =
            competitionTeamsDomainService.findByCompetitionId(competitionId);
        List<String> teamIds =
            competitionTeams.map(TeamCollection::getTeamIds).orElse(Collections.emptyList());
        
        List<Identity> teamIdentities = teamIds.stream()
                .map((id) -> new SimpleIdentity(id, competitionId.getOpus()))
                .collect(Collectors.toList());

        List<Team> teams = this.teamRepository.findAllById(teamIdentities);
        //setRelevantCompetition(teams, competitionId);
        return teams;
    }

    @Transactional
    public Optional<Team> findTeam(Identity teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            log.warn("Team with id {} not found.", teamId);
            return Optional.empty();
        }         
        return Optional.of(team);
    }

    /*
    private void setRelevantCompetition(List<Team> teams, Identity competitionId) {
        Optional<Competition> competition = this.competitionRepository.findById(competitionId);
        teams
                .forEach(team -> {
                    team.setCompetitionIds(new Identity[]{competitionId});
                    competition.ifPresent(c -> team.setCompetitionName(c.getName()));
                });
    }
    */

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam, int opus) {
        return internalCreateOrUpdateTeam(apiTeam, new TeamResponse.Player[0], opus);
    }

    private Team internalCreateOrUpdateTeam(
            ApiTeam apiTeam, TeamResponse.Player[] players, int opus) {
        SimpleIdentity identity = new SimpleIdentity(apiTeam.getId(), opus);
        Team team = newTeamOrFromDb(identity);
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, players, team, opus);
        }
        return team;
    }

    private Team newTeamOrFromDb(Identity identity) {
        Optional<Team> teamFromDb = teamRepository.findById(identity);
        Team team = teamFromDb.orElse(new Team(identity));
        return team;
    }

    public void createOrUpdateTeam(Team team) {
        Optional<Team> teamFromDb = teamRepository.findById(team.getId());
        if (teamFromDb.isEmpty()) {
            teamRepository.save(team);
        } else {
            Team oldTeam = teamFromDb.get();
            oldTeam.setPlayers(team.getPlayers());
            PopulatorUtil.copyNonNullProperties(team, oldTeam);
            teamRepository.save(oldTeam);
        }
    }
}
