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
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Team> findByCompetitionId(UUID competitionUuid) {
        Optional<CompetitionTeams> competitionTeams = competitionTeamsDomainService.findByCompetitionId(
                competitionUuid);
        List<UUID> teamUuids = competitionTeams.map(CompetitionTeams::getTeamUuids).orElse(Collections.emptyList());
        List<Team> teams = this.teamRepository.findAllById(teamUuids);
        setRelevantCompetition(teams, competitionUuid);
        return teams;
    }

    @Transactional
    public Optional<Team> findTeam(UUID teamUuid, Optional<UUID> competitionUuid) {
        List<Team> teams = teamRepository.findAllById(List.of(teamUuid));
        if (teams.size() == 1) {
            competitionUuid.ifPresent((uuid) -> setRelevantCompetition(teams, uuid));
            return Optional.of(teams.get(0));
        } else {
            return Optional.empty();
        }
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
