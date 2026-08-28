package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchInterpretation;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;

import java.util.Optional;

public abstract class AbstractMatchAdapter implements MatchAdapter {
    private final GameType game;
    private final StageMatchView.Capabilities capabilities;

    protected AbstractMatchAdapter(GameType game, StageMatchView.Capabilities capabilities) {
        this.game = game;
        this.capabilities = capabilities;
    }

    @Override
    public GameType game() {
        return game;
    }

    @Override
    public StageMatchView adapt(
            String stageId,
            StageSource source,
            Match match,
            MatchInterpretation interpretation) {
        StageMatchView.Score sourceScore = scoreFrom(match.getTeams());
        StageMatchView.Score officialScore = Optional.ofNullable(interpretation)
                .map(MatchInterpretation::getOfficialScore)
                .map(score -> new StageMatchView.Score(score.getHome(), score.getAway()))
                .orElse(sourceScore);

        return new StageMatchView(
                stageId,
                source.getId(),
                source.getGame(),
                source.getPlatform(),
                match.getId(),
                matchKey(match),
                match.getCompetitionId(),
                match.getStarted(),
                match.getFinished(),
                status(match),
                match.getTeams(),
                sourceScore,
                officialScore,
                match.isAdminResult(),
                match.isConcede(),
                match.isOvertime(),
                quality(match),
                capabilities,
                countingRules(interpretation),
                interpretation);
    }

    public static String matchKey(Match match) {
        if (match.getMatchId() != null && !match.getMatchId().isBlank()) {
            return match.getMatchId();
        }
        return match.getId() == null ? null : match.getId().getValue();
    }

    private StageMatchView.Score scoreFrom(Team[] teams) {
        if (teams == null || teams.length < 2) {
            return new StageMatchView.Score(null, null);
        }
        return new StageMatchView.Score(teams[0].getScore(), teams[1].getScore());
    }

    private String status(Match match) {
        if (Boolean.TRUE.equals(match.getIsFinalized())) {
            return "finalized";
        }
        if (match.getFinished() != null) {
            return "finished";
        }
        return "unknown";
    }

    private StageMatchView.Quality quality(Match match) {
        Team[] teams = match.getTeams();
        if (teams == null || teams.length < 2) {
            return StageMatchView.Quality.MINIMAL;
        }
        return match.getFinished() == null
                ? StageMatchView.Quality.PARTIAL
                : StageMatchView.Quality.COMPLETE;
    }

    private StageMatchView.CountingRules countingRules(MatchInterpretation interpretation) {
        MatchInterpretation.CountsFor counts = interpretation == null ? null : interpretation.getCountsFor();
        return counts == null
                ? new StageMatchView.CountingRules(true, true, true, true)
                : new StageMatchView.CountingRules(
                        counts.standingsOrDefault(),
                        counts.teamStatsOrDefault(),
                        counts.playerStatsOrDefault(),
                        counts.bracketOrDefault());
    }
}
