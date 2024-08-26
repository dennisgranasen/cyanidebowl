package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.export.naf.Coach;
import net.warp_scores.warpscores.export.naf.Game;
import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.export.naf.PlayerRecord;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@RequiredArgsConstructor
public class NafExporter {
    public static final int DEFAULT_TEAM_RATING = 100;

    public static final String AI_COACH_NAME = "ARTIFICIAL_INTELLIGENCE";

    public static final NafCoachLookupClient.NafCoach NONE_NAF_COACH = new NafCoachLookupClient.NafCoach("Non-NAF", 9,
            null);
    public static final String MULTIPLE_RACES = "Multiple Races";

    private final ContestService contestService;
    private final CompetitionService competitionService;
    private final NafCoachLookupClient nafCoachLookupClient;

    private final Map<String, NafCoachLookupClient.NafCoach> nafCoachNameCache = new HashMap<>();

    public Optional<NafReport> export(UUID competitionUuid, String exporterName) {
        Optional<Competition> competition = competitionService.loadCompetition(competitionUuid);
        return competition.flatMap(comp -> export(comp, exporterName));
    }

    public Optional<NafReport> export(Competition competition, String exporterName) {
        CompetitionStatus status = competition.getStatus();
        if (!CompetitionStatus.Finished.equals(status)) {
            log.warn("Will not export competition with status {}.", status);
            return Optional.empty();
        }

        List<Contest> competitionContests = contestService.getCompetitionContests(competition.getUuid());
        return Optional.of(export(competitionContests, exporterName));
    }

    NafReport export(List<Contest> competitionContests, String exporterName) {
        NafReport nafReport = new NafReport();
        nafReport.setCoaches(toCoachesFromContests(competitionContests));
        nafReport.setOrganiser(String.format("warp-scores.net//%s", exporterName));
        nafReport.setGames(toGames(competitionContests));
        return nafReport;
    }

    private List<Game> toGames(List<Contest> contests) {
        return contests
                .stream()
                .filter(this::noneArtificialIntelligenceGame)
                .filter(c -> c.getMatchDate() != null)
                .map(this::toGame)
                .toList();
    }

    private boolean noneArtificialIntelligenceGame(Contest contest) {
        return contest
                .getOpponents()
                .stream()
                .map(Team::getCoachName)
                .filter(AI_COACH_NAME::equals)
                .findFirst()
                .isEmpty();
    }

    private Game toGame(Contest contest) {
        Game game = new Game();
        game.setTimeStamp(contest.getMatchDate());
        game.setPlayerRecords(toPlayerRecords(contest.getOpponents()));
        return game;
    }

    private List<PlayerRecord> toPlayerRecords(List<Team> opponents) {
        return opponents
                .stream()
                .map(this::toPlayerRecord).toList();
    }

    private PlayerRecord toPlayerRecord(Team team) {
        PlayerRecord playerRecord = new PlayerRecord();
        playerRecord.setTeam(team.getRace().getNafRaceName());
        playerRecord.setName(lookupNafName(team.getCoachName()));
        playerRecord.setTeamRating(DEFAULT_TEAM_RATING);
        playerRecord.setNumber(lookupNafNumber(team.getCoachName()));
        playerRecord.setTouchDowns(team.getScore());
        playerRecord.setBadlyHurt(team.getInflictedcasualties());
        return playerRecord;
    }

    private List<Coach> toCoachesFromContests(List<Contest> contests) {
        List<Team> teams = contests
                .stream()
                .map(Contest::getOpponents)
                .flatMap(List::stream)
                .distinct()
                .toList();
        return toCoachesFromTeams(teams);
    }

    private List<Coach> toCoachesFromTeams(List<Team> teams) {
        Map<NafCoachLookupClient.NafCoach, String> raceNameByCoachName = teams.stream().collect(
                groupingBy(team -> lookupNafCoach(team.getCoachName()),
                        collectingAndThen(toList(), this::uniqueOrMultipleRacesQualifier)));

        return raceNameByCoachName
                .entrySet()
                .stream()
                .map(entry -> toCoach(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String uniqueOrMultipleRacesQualifier(List<Team> teams) {
        List<Race> distinctRaces = teams
                .stream()
                .map(Team::getRace)
                .distinct()
                .toList();
        return distinctRaces.size() == 1 ? distinctRaces.get(0).getNafRaceName() : MULTIPLE_RACES;
    }

    private Coach toCoach(NafCoachLookupClient.NafCoach nafCoach, String raceName) {
        Coach coach = new Coach();
        coach.setName(nafCoach.getNaf_name());
        coach.setTeam(raceName);
        coach.setNumber(nafCoach.getNaf_id());
        return coach;
    }

    private String lookupNafName(String coachName) {
        if (AI_COACH_NAME.equals(coachName)) {
            return NONE_NAF_COACH.getNaf_name();
        } else {
            return lookupNafCoach(coachName).getNaf_name();
        }
    }

    private Integer lookupNafNumber(String coachName) {
        if (AI_COACH_NAME.equals(coachName)) {
            return NONE_NAF_COACH.getNaf_id();
        } else {
            return lookupNafCoach(coachName).getNaf_id();
        }
    }

    private NafCoachLookupClient.NafCoach lookupNafCoach(String coachName) {
        return nafCoachNameCache.computeIfAbsent(coachName, this::loadNafCoach);
    }

    private NafCoachLookupClient.NafCoach loadNafCoach(String coachName) {
        log.info("Looking up {}...", coachName);
        NafCoachLookupClient.NafCoach nafCoach = nafCoachLookupClient.lookupNafCoach(coachName);
        if (StringUtils.hasText(nafCoach.getError()) || nafCoach.getNaf_id() == null) {
            log.info("No coach found...");
            return NONE_NAF_COACH;
        } else {
            log.info("Found {}...", nafCoach.getNaf_id());
            return nafCoach;
        }
    }
}
