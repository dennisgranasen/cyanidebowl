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
    public List<Team> findByCompetitionId(UUID competitionUuid, Optional<Integer> opus) {
        Optional<CompetitionTeams> competitionTeams = 
            competitionTeamsDomainService.findByCompetitionId(competitionUuid, opus);
        List<UUID> teamUuids = 
            competitionTeams.map(CompetitionTeams::getTeamUuids).orElse(Collections.emptyList());
        List<Team> teams = this.teamRepository.findAllById(teamUuids);
        setRelevantCompetition(teams, competitionUuid);
        return teams;
    }

    @Transactional
    public List<Team> findByOldCompetitionId(Integer competitionId, Optional<Integer> opus) {
        CompetitionTeams competitionTeams = 
            competitionTeamsDomainService.findByOldCompetitionId(competitionId, opus);
        List<UUID> teamUuids = competitionTeams.getTeamUuids();
        List<Team> teams = this.teamRepository.findAllById(teamUuids);
        setRelevantCompetition(teams, competitionId, opus.orElse(defaultOpus));
        return teams;
    }


    @Transactional
    public Optional<Team> findTeam(UUID teamUuid, Optional<UUID> competitionUuid) {
        List<Team> teams = teamRepository.findAllById(List.of(teamUuid));
        if (teams.size() == 1) {
            competitionUuid.ifPresent((uuid) -> setRelevantCompetition(teams, uuid));
            Team team = teams.get(0);
            UUID competitionId = team.getCompetitionIds()[0];
            Optional<Competition> competition = competitionRepository.findById(competitionId);
            team.setLeagueName(competition.map(Competition::getLeagueName).orElse(null));
            Optional<UUID> leagueId = competition.map(Competition::getLeagueId);
            team.setLeagueIds(leagueId.map(id -> new UUID[]{id}).orElse(null));
            leagueId.ifPresent(id ->
                    officialLeagueAndCompetitions.adjustCompetitionName(id, team.getCompetitionName(),
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

    private void setRelevantCompetition(List<Team> teams, UUID competitionUuid) {
        Optional<Competition> competition = this.competitionRepository.findById(competitionUuid);
        teams
                .forEach(team -> {
                    team.setCompetitionIds(new UUID[]{competitionUuid});
                    competition.ifPresent(c -> team.setCompetitionName(c.getName()));
                });
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam) {
        Team team = newTeamOrFromDb(uuidConverter.toUuid(apiTeam.getId()), apiTeam.getName());
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, new TeamResponse.Player[0], team);
        }
        return team;
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam, TeamResponse.Player[] players) {
        Team team = newTeamOrFromDb(uuidConverter.toUuid(apiTeam.getId()), apiTeam.getName());
        if (team != null) {
            teamPopulator.populateTeamTeam(apiTeam, players, team);
        }
        return team;
    }

    private Team newTeamOrFromDb(Optional<UUID> uuid, String name) {
        if (uuid.isEmpty()) {
            log.error("Can't convert team '{}'. Need an UUID.", name);
            return null;
        }
        Optional<Team> teamFromDb = uuid.flatMap(teamRepository::findById);
        Team team = teamFromDb.orElse(new Team());
        team.setId(uuid.get());
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
