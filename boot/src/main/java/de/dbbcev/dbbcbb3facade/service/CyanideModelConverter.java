package de.dbbcev.dbbcbb3facade.service;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiCoach;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiCompetition;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiContest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiLeague;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiMatch;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiTeam;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.CompetitionsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.ContestsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.MatchResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.MatchesResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.StatusResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.TeamResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.TeamsResponse;
import de.dbbcev.dbbcbb3facade.domain.TeamRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.Contest;
import de.dbbcev.dbbcbb3facade.domain.model.League;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
import de.dbbcev.dbbcbb3facade.domain.model.Player;
import de.dbbcev.dbbcbb3facade.domain.model.Status;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    public List<Team> createOrUpdateTeams(TeamsResponse teamsResponse) {
        if (teamsResponse == null || teamsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(teamsResponse.getTeams())
                .map(this::createOrUpdateTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Team createOrUpdateTeam(ApiTeam apiTeam) {
        Team team = createOrUpdateTeam(getAsUuid(apiTeam.getId()), apiTeam.getName());
        populateTeam(team, apiTeam);
        return team;
    }

    private void populateTeam(Team team, ApiTeam apiTeam) {
        team.setCoachName(apiTeam.getCoach());
        team.setId(getAsUuid(apiTeam.getId()).orElse(null));
        //team.setCoachId(apiTeam.getCoach_id());
        BeanUtils.copyProperties(apiTeam, team);
        team.setCompetitionIds(toUuids(team.getCompetitionIds(), apiTeam.getBb3_competition_id()));
    }

    private Team createOrUpdateTeam(Optional<UUID> id, String name) {
        if (id.isEmpty()) {
            log.error("Can't convert team '{}'. Need an UUID.", name);
            return null;
        }
        Optional<Team> teamFromDb = teamRepository.findById(id.get());
        Team team = teamFromDb.orElse(new Team());
        team.setId(id.get());
        team.setName(name);
        return team;
    }

    public Team createOrUpdateTeam(TeamResponse teamResponse) {
        if (teamResponse == null) {
            return null;
        }
        ApiTeam apiTeam = teamResponse.getTeam();
        String id = apiTeam.getId();
        Optional<UUID> uuid = getAsUuid(id);
        Optional<Team> teamFromDb = uuid
                .map(teamRepository::findById)
                .orElse(Optional.empty());
        Team team = teamFromDb.orElse(createOrUpdateTeam(uuid, apiTeam.getName()));
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

    public Optional<UUID> getAsUuid(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (Exception ex) {
            log.error("Not an UUID? (value: {}).", id);
            return Optional.empty();
        }
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
        player.setId(getAsUuid(apiPlayer.getId()).get());
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

    private UUID[] toUuids(UUID[] existingUuids, String bb3CompetitionId) {
        if (bb3CompetitionId == null) {
            return existingUuids;
        }
        String[] competitions = bb3CompetitionId.split(",");
        Set<UUID> uuids = Arrays.stream(existingUuids).collect(Collectors.toSet());
        uuids.addAll(Arrays.stream(competitions)
                .map(UUID::fromString)
                .collect(Collectors.toSet()));
        return uuids.toArray(new UUID[0]);
    }

    public Match toMatch(MatchResponse matchResponse) {
        ApiMatch apiMatch = matchResponse.getMatch();
        if (apiMatch.getId() == null) {
            return null;
        }

        Match match = newMatch(apiMatch);
        match.setCoaches(
                Arrays.stream(apiMatch.getCoaches())
                        .map(this::toCoach)
                        .collect(Collectors.toList()));
        match.setTeams(
                Arrays.stream(apiMatch.getTeams())
                        .map(this::toTeam)
                        .collect(Collectors.toList()));
        return match;
    }

    private Match newMatch(ApiMatch apiMatch) {
        Match match = new Match();
        match.setMatchId(
                getNonNull(apiMatch.getMatchUuid(), apiMatch.getUuid(), getAsUuid(apiMatch.getId()).orElse(null)));
        match.setFinished(apiMatch.getFinished());
        match.setStarted(apiMatch.getStarted());
        match.setRound(apiMatch.getRound());
        match.setCompetitionName(apiMatch.getCompetitionname());
        match.setCompetitionId(apiMatch.getIdcompetition());
        match.setLeagueId(apiMatch.getIdleague());
        match.setLeagueName(apiMatch.getLeaguename());
        match.setStadium(apiMatch.getStadium());
        return match;
    }

    private UUID getNonNull(UUID... uuids) {
        if (uuids == null) {
            throw new NoSuchElementException(
                    String.format("Can't get a non null value from null input (input: %s).", uuids));
        }
        List<UUID> uniqueUuids = Arrays
                .stream(uuids)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (uniqueUuids.size() == 0) {
            throw new NoSuchElementException(String.format("All ids null (%s).", Arrays.asList(uuids)));
        }
        if (uniqueUuids.size() > 1) {
            throw new IllegalArgumentException(String.format("Got ambiguous ids (%s).", uniqueUuids));
        }
        return uniqueUuids.get(0);
    }

    private Match.Coach toCoach(ApiCoach apiCoach) {
        Match.Coach coach = new Match.Coach();
        coach.setId(apiCoach.getId());
        coach.setName(apiCoach.getName());
        return coach;
    }

    private Team toTeam(ApiTeam apiTeam) {
        Team team = new Team();
        populateTeam(team, apiTeam);
        return team;
    }

    public List<Match> toMatches(MatchesResponse matchesResponse) {
        ApiMatch[] apiMatches = matchesResponse.getMatches();
        if (apiMatches == null) {
            return Collections.emptyList();
        }

        List<Match> matches = Arrays
                .stream(apiMatches)
                .map(this::toMatch)
                .collect(Collectors.toList());
        return matches;
    }

    public Match toMatch(ApiMatch apiMatch) {
        Match match = newMatch(apiMatch);
        match.setCoaches(
                Arrays.stream(apiMatch.getCoaches())
                        .map(this::toCoach)
                        .collect(Collectors.toList()));
        match.setTeams(
                Arrays.stream(apiMatch.getTeams())
                        .map(this::toTeam)
                        .collect(Collectors.toList()));
        return match;
    }

    public List<Contest> toContests(ContestsResponse contestsResponse) {
        if (contestsResponse == null || contestsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        ApiContest[] upcomingMatches = contestsResponse.getUpcoming_matches();
        if (upcomingMatches == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(upcomingMatches)
                .map(this::toContest)
                .collect(Collectors.toList());
    }

    private Contest toContest(ApiContest apiContestMatch) {
        Contest contest = new Contest();
        contest.setContestUuid(apiContestMatch.getContest_id());
        contest.setFormat(apiContestMatch.getFormat());
        contest.setLeagueId(apiContestMatch.getLeague_id());
        contest.setCompetitionId(apiContestMatch.getCompetition_id());
        contest.setCompetitionName(apiContestMatch.getCompetition());
        contest.setLeagueName(apiContestMatch.getLeague());
        contest.setStadium(apiContestMatch.getStadium());
        contest.setType(apiContestMatch.getType());
        contest.setMatchId(apiContestMatch.getMatch_id());
        contest.setMatchDate(apiContestMatch.getMatch_date());
        contest.setMatchUuid(apiContestMatch.getMatch_uuid());
        contest.setLive(apiContestMatch.getLive());
        contest.setOpponents(toOpponents(apiContestMatch.getOpponents()));
        contest.setStatus(apiContestMatch.getStatus());
        contest.setRound(apiContestMatch.getRound());
        contest.setWinner(apiContestMatch.getWinner());
        return contest;
    }

    private List<Team> toOpponents(ApiContest.Opponent[] apiOpponents) {
        if (apiOpponents == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiOpponents).map(this::toOpponent).collect(Collectors.toList());
    }

    private Team toOpponent(ApiContest.Opponent apiOpponent) {
        Team team = new Team();
        ApiContest.Team apiTeam = apiOpponent.getTeam();
        team.setCoachId(apiOpponent.getCoach().getId().toString());
        team.setCoachName(apiOpponent.getCoach().getName());
        team.setId(apiTeam.getId());
        team.setName(apiTeam.getName());
        team.setFraction(apiTeam.getRace());
        team.setScore(apiTeam.getScore());
        team.setDeath(apiTeam.getDeath());
        team.setLogo(apiTeam.getLogo());
        team.setValue(apiTeam.getValue());

        return team;
    }

    public List<Competition> toCompetitions(CompetitionsResponse competitionsResponse) {
        if (competitionsResponse == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(competitionsResponse.getCompetitions())
                .map(this::toCompetition)
                .collect(Collectors.toList());
    }

    private Competition toCompetition(ApiCompetition apiCompetition) {
        Competition competition = new Competition();
        competition.setUuid(UUID.fromString(apiCompetition.getId()));
        competition.setLeagueId(UUID.fromString(apiCompetition.getLeague().getId()));
        competition.setLeagueLogo(apiCompetition.getLeague().getLogo());
        competition.setLogo(apiCompetition.getLogo());
        competition.setDateCreated(apiCompetition.getDate_created());
        competition.setFormat(apiCompetition.getFormat());
        competition.setStatus(apiCompetition.getStatus_name());
        competition.setName(apiCompetition.getName());
        competition.setLeagueName(apiCompetition.getLeague().getName());
        competition.setRound(apiCompetition.getRound());
        competition.setRoundsCount(apiCompetition.getRounds_count());
        competition.setTeamsCount(apiCompetition.getTeams_count());
        competition.setTeamsMax(apiCompetition.getTeams_max());
        competition.setTimeBonusDuration(apiCompetition.getTime_bonus_duration());
        competition.setTurnDuration(apiCompetition.getTurn_duration());
        return competition;
    }

    public League toLeague(ApiLeague apiLeague) {
        League league = new League();
        league.setUuid(UUID.fromString(apiLeague.getId()));
        league.setLogo(apiLeague.getLogo());
        league.setName(apiLeague.getName());
        league.setTeamCount(apiLeague.getTeam_count());
        league.setTreasury(apiLeague.getTreasury());
        league.setDateLastMatch(apiLeague.getDate_last_match());
        return league;
    }
}
