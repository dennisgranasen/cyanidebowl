package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.League;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class LookupResponse extends ApiResponse {
    private IdWithName[] teams;
    private IdWithName[] leagues;
    private IdWithName[] coaches;
    private IdWithName[] competitions;

    private League[] fullLeagues;
    private Competition[] fullCompetitions;

    @Override
    public void updateChangeableAttribute() {
        updateChangeableAttributeTo(true);
    }

    @Override
    public String getInformationString() {
        return String.format("CompetitionsResponse[isEmpty=%s, competitions=%s, leagues=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(competitions).map(c -> String.valueOf(c.length)).orElse("n/a"),
                Optional.ofNullable(leagues).map(l -> String.valueOf(l.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
