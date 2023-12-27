package de.dbbcev.dbbcbb3facade.service;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.Race;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.StatusResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.competitions.CompetitionsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.contests.ContestsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues.LeagueResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.matches.MatchesResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.teams.TeamResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.teams.TeamsResponse;
import de.dbbcev.dbbcbb3facade.domain.TeamRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.Contest;
import de.dbbcev.dbbcbb3facade.domain.model.League;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
import de.dbbcev.dbbcbb3facade.domain.model.Player;
import de.dbbcev.dbbcbb3facade.domain.model.Status;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CyanideModelConverter {
    private final TeamRepository teamRepository;

    public CyanideModelConverter(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Status toStatus(StatusResponse.Game game) {
        Status status = new Status();
        status.setGameName(game.getName());
        StatusResponse.ServiceStatuses serviceStatuses = game.getService_statuses();
        status.setGameServerDatabase(serviceStatuses.isGame_server_database());
        status.setGameServerAddressDirectory(serviceStatuses.isGame_server_address_directory());
        status.setMaintenance(toMaintenance(game.getMaintenance()));
        status.setSocialLinks(game.getSocial_links());

        return status;
    }

    private Status.Maintenance toMaintenance(StatusResponse.Maintenance responseMaintenance) {
        Status.Maintenance maintenance = new Status.Maintenance();
        maintenance.setPc(responseMaintenance.getPc());
        maintenance.setMicrosoft(responseMaintenance.getMicrosoft());
        maintenance.setSony(responseMaintenance.getSony());
        return maintenance;
    }

    public List<Team> toTeams(TeamsResponse teamsResponse) {
        return Arrays.stream(teamsResponse.getTeams())
                .map(this::toTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Team toTeam(TeamsResponse.Team apiTeam) {
        Team team = toTeam(Optional.of(apiTeam.getId()), apiTeam.getTeam());
        team.setCoachName(apiTeam.getCoach());
        // team.setCoachId(apiTeam.getCoach_id());
        team.setMotto(apiTeam.getDescription());
        team.setLogo(apiTeam.getLogo());
        team.setLeagueName(apiTeam.getLeague());
        team.setLeagueId(apiTeam.getLeague_id());
        team.setCompetitionName(apiTeam.getBb3_competition());
        team.setCompetitionId(getCurrentCompetitionIdFrom(apiTeam.getBb3_competition_id()));
        Race race = apiTeam.getRace();
        if (race == null && apiTeam.getRace_id() != null) {
            race = Race.forValue(apiTeam.getRace_id());
        }
        team.setFraction(race);
        team.setDateLastMatch(apiTeam.getDateLastMatch());
        return team;
    }

    private Team toTeam(Optional<UUID> id, String name) {
        if (id.isEmpty()) {
            log.error("Can't convert team '{}'. Need an UUID.", name);
            return null;
        }
        Team team = new Team();
        team.setId(id.get());
        team.setName(name);
        return team;
    }

    public Team toTeam(TeamResponse teamResponse) {
        if (teamResponse == null) {
            return null;
        }
        TeamResponse.Team apiTeam = teamResponse.getTeam();
        Optional<Team> teamFromDb = apiTeam
                .getIdAsUUIDOrNull()
                .map(teamRepository::findById)
                .orElse(Optional.empty());
        Team team = teamFromDb.orElse(toTeam(apiTeam.getIdAsUUIDOrNull(), apiTeam.getName()));
        if (team == null) {
            return null;
        }
        team.setCoachId(teamResponse.getCoach().getId());
        team.setCoachName(teamResponse.getCoach().getName());
        team.setCash(apiTeam.getCash());
        team.setValue(apiTeam.getValue());

        TeamResponse.Player[] apiPlayers = teamResponse.getRoster();
        List<Player> players = toPlayers(apiPlayers);
        team.setPlayers(players);
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
        player.setName(apiPlayer.getName());
        player.setId(apiPlayer.getIdAsUUIDOrNull().get());
        player.setNumber(apiPlayer.getNumber());
        player.setType(apiPlayer.getType());
        player.setXp(apiPlayer.getXp());
        player.setValue(apiPlayer.getValue());
        player.setRaceId(apiPlayer.getIdraces());
        player.setSkills(apiPlayer.getSkills());
        player.setSuspendedNextMatch(apiPlayer.getSuspended_next_match());
        player.setAttributes(toAttributes(apiPlayer.getAttributes()));
        player.setCasualtiesStateIds(apiPlayer.getCasualties_state_id());
        player.setCasualtiesStates(apiPlayer.getCasualties_state());
        return player;
    }

    private Player.Attributes toAttributes(TeamResponse.Player.Attributes apiAttributes) {
        Player.Attributes attributes = new Player.Attributes();
        attributes.setAg(apiAttributes.getAg());
        attributes.setAv(apiAttributes.getAv());
        attributes.setSt(apiAttributes.getSt());
        attributes.setMa(apiAttributes.getMa());
        attributes.setPa(apiAttributes.getPa());
        return attributes;
    }

    private UUID getCurrentCompetitionIdFrom(String bb3CompetitionId) {
        String[] competitions = bb3CompetitionId.split(",");
        return UUID.fromString(competitions[competitions.length - 1]);
    }

    public List<Match> toMatches(MatchesResponse matchesResponse) {
        if (matchesResponse == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(matchesResponse.getMatches())
                .map(this::toMatch)
                .collect(Collectors.toList());
    }

    private Match toMatch(MatchesResponse.Match apiMatch) {
        Match match = new Match();
        match.setMatchId(apiMatch.getUuid());
        match.setFinished(apiMatch.getFinished());
        match.setStarted(apiMatch.getStarted());
        match.setRound(apiMatch.getRound());
        match.setCompetitionName(apiMatch.getCompetitionname());
        match.setCompetitionId(apiMatch.getIdcompetition());
        match.setLeagueId(apiMatch.getIdleague());
        match.setLeagueName(apiMatch.getLeaguename());
        match.setStadium(apiMatch.getStadium());
        match.setCoaches(
                Arrays.stream(apiMatch.getCoaches())
                        .map(c -> c.getIdAsUUIDOrNull().get())
                        .collect(Collectors.toList()));
        match.setTeams(
                Arrays.stream(apiMatch.getTeams())
                        .map(t -> t.getIdAsUUIDOrNull().get())
                        .collect(Collectors.toList()));
        return match;
    }

    public List<Contest> toContests(ContestsResponse contestsResponse) {
        ContestsResponse.Match[] upcomingMatches = contestsResponse.getUpcoming_matches();
        if (upcomingMatches == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(upcomingMatches)
                .map(this::toContest)
                .collect(Collectors.toList());
    }

    private Contest toContest(ContestsResponse.Match apiContestMatch) {
        Contest contest = new Contest();

        contest.setContestUuid(apiContestMatch.getContest_id());
        return contest;
    }

    public List<Competition> toCompetitions(CompetitionsResponse competitionsResponse) {
        if (competitionsResponse == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(competitionsResponse.getCompetitions())
                .map(this::toCompetition)
                .collect(Collectors.toList());
    }

    private Competition toCompetition(CompetitionsResponse.Competition apiCompetition) {
        Competition competition = new Competition();
        competition.setUuid(apiCompetition.getId());
        competition.setLeagueId(apiCompetition.getLeague().getId());
        competition.setDateCreated(apiCompetition.getDate_created());
        competition.setFormat(apiCompetition.getFormat());
        competition.setStatus(apiCompetition.getStatus_name());
        competition.setName(apiCompetition.getName());
        competition.setRound(apiCompetition.getRound());
        competition.setRoundsCount(apiCompetition.getRounds_count());
        competition.setTeamsCount(apiCompetition.getTeams_count());
        competition.setTeamsMax(apiCompetition.getTeams_max());
        competition.setTimeBonusDuration(apiCompetition.getTime_bonus_duration());
        competition.setTurnDuration(apiCompetition.getTurn_duration());
        return competition;
    }

    public League toLeague(LeagueResponse.League apiLeague) {
        League league = new League();
        league.setUuid(apiLeague.getId());
        league.setLogo(apiLeague.getLogo());
        league.setName(apiLeague.getName());
        league.setTeamCount(apiLeague.getTeam_count());
        league.setTreasury(apiLeague.getTreasury());
        league.setDateLastMatch(apiLeague.getDate_last_match());
        return league;
    }
}
