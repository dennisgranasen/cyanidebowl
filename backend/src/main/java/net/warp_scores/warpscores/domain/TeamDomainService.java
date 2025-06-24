package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.TeamCollection;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import net.warp_scores.warpscores.service.UUIDConverter;
import net.warp_scores.warpscores.service.IdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamDomainService {
    private final TeamRepository teamRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamPopulator teamPopulator;
    private final TeamCollectionDomainService competitionTeamsDomainService;
    private final OfficialLeagueAndCompetitions officialLeagueCompetitions;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

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
        setRelevantCompetition(teams, competitionId);
        return teams;
    }

    @Transactional
    public Optional<Team> findTeam(Identity teamId, Optional<Identity> competitionId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            log.warn("Team with id {} not found.", teamId);
            return Optional.empty();
        } 
        
        competitionId.ifPresent((id) -> setRelevantCompetition(Collections.singletonList(team), id));
        
        Identity competitionId0 = team.getCompetitionIds()[0];
        Optional<Competition> competition = competitionRepository.findById(competitionId0);
        team.setLeagueName(competition.map(Competition::getLeagueName).orElse(null));
        team.setLeagueIds(new Identity[]{competition.map( c -> 
            c.getLeagueId()).orElse(null)});
        competition.map(Competition::getLeagueId).ifPresent(id ->
                officialLeagueAndCompetitions.adjustCompetitionName(
                    id, team.getCompetitionName(),
                    team::setCompetitionName));
        return Optional.of(team);
    }

    private void setRelevantCompetition(List<Team> teams, Identity competitionId) {
        Optional<Competition> competition = this.competitionRepository.findById(competitionId);
        teams
                .forEach(team -> {
                    team.setCompetitionIds(new Identity[]{competitionId});
                    competition.ifPresent(c -> team.setCompetitionName(c.getName()));
                });
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam, int opus) {

        SimpleIdentity identity = new SimpleIdentity(apiTeam.getId(), opus);
        Team team = newTeamOrFromDb(identity);
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, new TeamResponse.Player[0], team, opus);
        }
        return team;
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
