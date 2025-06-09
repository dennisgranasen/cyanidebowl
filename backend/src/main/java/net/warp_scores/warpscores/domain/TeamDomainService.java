package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionTeams;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import net.warp_scores.warpscores.service.UUIDConverter;
import net.warp_scores.warpscores.service.IdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamDomainService {
    private final TeamRepository teamRepository;

    private final CompetitionRepository competitionRepository;

    private final TeamPopulator teamPopulator;

    private final UUIDConverter uuidConverter;
    private final CompetitionTeamsDomainService competitionTeamsDomainService;
    private final IdService idService;
    private final OfficialLeagueAndCompetitions officialLeagueCompetitions;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;
    
    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public List<Team> createOrUpdateTeams(TeamsResponse teamsResponse) {
        if (teamsResponse == null || teamsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        List<Team> teams = Arrays.stream(teamsResponse.getTeams())
                .map(this::internalCreateOrUpdateTeam)
                .collect(Collectors.toList());
        return teamRepository.saveAll(teams);
    }

    @Transactional
    public Team createOrUpdateTeam(TeamResponse teamResponse) {
        if (teamResponse == null || teamResponse.isEmpty()) {
            return null;
        }
        Team team = internalCreateOrUpdateTeam(teamResponse.getTeam(), teamResponse.getRoster());
        return teamRepository.save(team);
    }

    @Transactional
    public List<Team> findByCompetitionId(String competitionId, Optional<Integer> opus) {
        Optional<CompetitionTeams> competitionTeams = 
            competitionTeamsDomainService.findByCompetitionId(competitionId, opus);
        List<String> teamIds = 
            competitionTeams.map(CompetitionTeams::getTeamIds).orElse(Collections.emptyList());

        // Compose Mongo IDs for lookup
        List<String> mongoIds = teamIds.stream()
            .map(id -> idService.getComposedId(opus, id))
            .collect(Collectors.toList());

        List<Team> teams = this.teamRepository.findAllById(mongoIds);
        setRelevantCompetition(teams, competitionId);
        return teams;
    }


    @Transactional
    public Optional<Team> findTeam(String teamId, Optional<String> competitionId, Optional<Integer> opus) {
        String mongoId = idService.getComposedId(opus, teamId);
        List<Team> teams = teamRepository.findAllById(List.of(mongoId));
        if (teams.size() == 1) {
            competitionId.ifPresent((id) -> setRelevantCompetition(teams, id));
            Team team = teams.get(0);
            String competitionId0 = team.getCompetitionIds()[0];
            Optional<Competition> competition = competitionRepository.findById(competitionId0);
            team.setLeagueName(competition.map(Competition::getLeagueName).orElse(null));
            Optional<String> leagueId = competition.map(Competition::getLeagueId);
            team.setLeagueIds(new String[]{leagueId.get()});
            leagueId.ifPresent(id ->
                    officialLeagueAndCompetitions.adjustCompetitionName(
                        id, team.getCompetitionName(), 
                        team::setCompetitionName));
            return Optional.of(team);
        } else {
            return Optional.empty();
        }
    }

    private void setRelevantCompetition(List<Team> teams, Integer oldCompetitionId, Integer opus) {
        /*  
        TODO: Not implemented yet, but maybe needed for old competitions.
            Need to determine what the purpose of relevant competition is in this context.
        Optional<Competition> competition = 
            this.competitionRepository.findByOldIdAndOpus(oldCompetitionId, opus);
        teams
                .forEach(team -> {
                    UUID
                    team.setCompetitionIds(new UUID[]{competitionUuid});
                    competition.ifPresent(c -> team.setCompetitionName(c.getName()));
                });
                */
    }

    private void setRelevantCompetition(List<Team> teams, String competitionId) {
        Optional<Competition> competition = this.competitionRepository.findById(competitionId);
        teams
                .forEach(team -> {
                    team.setCompetitionIds(new String[]{competitionId});
                    competition.ifPresent(c -> team.setCompetitionName(c.getName()));
                });
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam) {
        Team team = newTeamOrFromDb(Optional.ofNullable(apiTeam.getId()), apiTeam.getName());
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, new TeamResponse.Player[0], team);
        }
        return team;
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam, TeamResponse.Player[] players) {
        Team team = newTeamOrFromDb(Optional.ofNullable(apiTeam.getId()), apiTeam.getName());
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, players, team);
        }
        return team;
    }

    private Team newTeamOrFromDb(Optional<String> id, String name) {
        if (id.isEmpty()) {
            log.error("Can't convert team '{}'. Need an ID.", name);
            return null;
        }
        Optional<Team> teamFromDb = id.flatMap(teamRepository::findById);
        Team team = teamFromDb.orElse(new Team());
        team.setId(id.get());
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
