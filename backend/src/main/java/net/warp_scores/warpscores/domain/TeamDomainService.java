package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import net.warp_scores.warpscores.domain.model.Player;
import net.warp_scores.warpscores.domain.model.Team;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
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

    private final TeamPopulator teamPopulator;

    private final UUIDConverter uuidConverter;

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
        Team team = internalCreateOrUpdateTeam(teamResponse.getTeam());
        TeamResponse.Player[] apiPlayers = teamResponse.getRoster();
        List<Player> players = toPlayers(apiPlayers);
        team.setPlayers(players);
        return teamRepository.save(team);
    }

    @Transactional
    public List<Team> findByLeagueId(UUID leagueUuid) {
        return this.teamRepository.findByLeagueId(leagueUuid);
    }

    @Transactional
    public List<Team> findByCompetitionId(UUID competitionId) {
        List<Team> teams = this.teamRepository.findByCompetitionId(competitionId);
        setRelevantCompetition(teams, competitionId);
        return teams;
    }

    @Transactional
    public Optional<Team> findTeam(UUID teamUuid, Optional<UUID> competitionUuid) {
        List<Team> teams = teamRepository.findAllById(Arrays.asList(teamUuid));
        if (teams.size() == 1) {
            competitionUuid.ifPresent((uuid) -> setRelevantCompetition(teams, uuid));
            return Optional.of(teams.get(0));
        } else {
            return Optional.empty();
        }
    }

    private void setRelevantCompetition(List<Team> teams, UUID competitionUuid) {
        teams
                .stream()
                .forEach(team -> team.setCompetitionIds(new UUID[]{competitionUuid}));
    }

    private Team internalCreateOrUpdateTeam(ApiTeam apiTeam) {
        Team team = newTeamOrFromDb(uuidConverter.toUuid(apiTeam.getId()), apiTeam.getName());
        if (team != null && team.isUpdateableFromApi()) {
            teamPopulator.populateTeam(apiTeam, team);
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

    private List<Player> toPlayers(TeamResponse.Player[] apiPlayers) {
        if (apiPlayers == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiPlayers).map(this::toPlayer).collect(Collectors.toList());
    }

    private Player toPlayer(TeamResponse.Player apiPlayer) {
        Player player = new Player();
        PopulatorUtil.copyNonNullProperties(apiPlayer, player);
        player.setId(uuidConverter.toUuid(apiPlayer.getId()).orElse(null));
        player.setRaceId(apiPlayer.getIdraces());
        player.setSuspendedNextMatch(apiPlayer.getSuspended_next_match());
        player.setAttributes(toAttributes(apiPlayer.getAttributes()));
        player.setCasualtiesStateIds(apiPlayer.getCasualties_state_id());
        player.setCasualtiesStates(apiPlayer.getCasualties_state());
        return player;
    }

    private Player.Attributes toAttributes(TeamResponse.Player.Attributes apiAttributes) {
        Player.Attributes attributes = new Player.Attributes();
        PopulatorUtil.copyNonNullProperties(apiAttributes, attributes);
        return attributes;
    }

}
