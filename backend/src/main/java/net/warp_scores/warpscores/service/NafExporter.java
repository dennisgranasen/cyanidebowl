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
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NafExporter {
    public static final int DEFAULT_TEAM_RATING = 100;

    private final ContestService contestService;

    public Optional<NafReport> export(Competition competition, String exporterName) {
        CompetitionStatus status = competition.getStatus();
        if (CompetitionStatus.Finished.equals(status)) {
            log.warn("Will not export competition with status {}.", status);
            return Optional.empty();
        }

        List<Contest> competitionContests = contestService.getCompetitionContests(competition.getUuid());
        NafReport nafReport = new NafReport();
        nafReport.setCoaches(toCoaches(competitionContests));
        nafReport.setOrganizer(String.format("warp-scores.net//%s", exporterName));
        nafReport.setGames(toGames(competitionContests));
        return Optional.of(nafReport);
    }

    private List<Game> toGames(List<Contest> contests) {
        return contests
                .stream()
                .map(this::toGame)
                .toList();
    }

    private Game toGame(Contest contest) {
        Game game = new Game();
        game.setTimeStamp(contest.getMatchDate());
        game.setPlayerRecords(toPlayerRecords(contest.getOpponents()));
        return game;
    }

    private List<PlayerRecord> toPlayerRecords(List<Team> opponents) {
        return opponents.stream().map(this::toPlayerRecord).toList();
    }

    private PlayerRecord toPlayerRecord(Team team) {
        PlayerRecord playerRecord = new PlayerRecord();
        playerRecord.setTeam(team.getRace().getRaceName());
        playerRecord.setName(team.getCoachName());
        playerRecord.setTeamRating(DEFAULT_TEAM_RATING);
        playerRecord.setNumber(lookupNafNumber(team.getCoachName()));
        playerRecord.setTouchDowns(team.getScore());
        playerRecord.setBadlyHurt(team.getInflictedcasualties());
        return playerRecord;
    }

    private List<Coach> toCoaches(List<Contest> contests) {
        List<Coach> coaches = contests
                .stream()
                .map(Contest::getOpponents)
                .flatMap(List::stream)
                .distinct()
                .map(this::toCoach)
                .toList();
        return coaches;
    }

    private Coach toCoach(Team team) {
        Coach coach = new Coach();
        coach.setName(team.getCoachName());
        coach.setTeam(team.getRace().getRaceName());
        coach.setNumber(lookupNafNumber(team.getCoachName()));
        return coach;
    }

    private Integer lookupNafNumber(String coachName) {
        return null;
    }
}
