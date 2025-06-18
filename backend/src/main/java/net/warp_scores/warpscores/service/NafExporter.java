package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.export.naf.Coach;
import net.warp_scores.warpscores.export.naf.Game;
import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.export.naf.PlayerRecord;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.NafCoach;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.warp_scores.warpscores.service.NafCoachService.BB3_AI_COACH_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class NafExporter {
    public static final int DEFAULT_TEAM_RATING = 100;

    public static final String MULTIPLE_RACES = "Multiple Races";

    private final ContestService contestService;
    private final CompetitionService competitionService;
    private final NafCoachService nafCoachService;

    public Optional<NafReport> export(Identity competitionId, String exporterName) {
        Optional<Competition> competition = 
            competitionService.loadCompetition(competitionId);
        return competition.flatMap(comp -> export(comp, exporterName));
    }

    public Optional<NafReport> export(Competition competition, String exporterName) {
        CompetitionStatus status = competition.getStatus();
        if (!CompetitionStatus.Finished.equals(status)) {
            log.warn("Will not export competition with status {}.", status);
            return Optional.empty();
        }

        List<Contest> competitionContests = 
            contestService.getCompetitionContests(competition.getIdentity(), Optional.empty());
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
                .filter(BB3_AI_COACH_NAME::equals)
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
                .map(this::toPlayerRecord)
                .filter(Objects::nonNull)
                .toList();
    }

    private PlayerRecord toPlayerRecord(Team team) {
        Optional<NafCoach> nafCoach = nafCoachService.lookupCoach(team.getCoachName());
        return nafCoach.map(coach -> toPlayerRecord(team, coach)).orElse(null);
    }

    private PlayerRecord toPlayerRecord(Team team, NafCoach nafCoach) {
        PlayerRecord playerRecord = new PlayerRecord();
        playerRecord.setTeam(team.getRace().getNafRaceName());
        playerRecord.setName(nafCoach.getNafName());
        playerRecord.setTeamRating(DEFAULT_TEAM_RATING);
        playerRecord.setNumber(nafCoach.getNafId());
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
        Map<Optional<NafCoach>, String> raceNameByCoachName = teams
                .stream()
                .collect(groupingBy(team -> nafCoachService.lookupCoach(team.getCoachName()),
                        collectingAndThen(toList(), this::uniqueOrMultipleRacesQualifier)));

        return raceNameByCoachName
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().isPresent())
                .sorted(Comparator.comparing(e -> e.getKey().get().getNafName()))
                .map(entry -> toCoach(entry.getKey().get(), entry.getValue()))
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

    private Coach toCoach(NafCoach nafCoach, String raceName) {
        Coach coach = new Coach();
        coach.setName(nafCoach.getNafName());
        coach.setTeam(raceName);
        coach.setNumber(nafCoach.getNafId());
        return coach;
    }
}
