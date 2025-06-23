package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.League;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class DetailedLookupResponse extends LookupResponse {
    private League[] leagueDetails;
    private Competition[] competitionDetails;

    @Override
    public String getInformationString() {
        return String.format("DetailedCompetitionsResponse[isEmpty=%s, competitions=%s, leagues=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(getCompetitions()).map(c -> String.valueOf(c.length)).orElse("n/a"),
                Optional.ofNullable(getLeagues()).map(l -> String.valueOf(l.length)).orElse("n/a"),
                isChangeableResponse());
    }

    @Override
    public void updateChangeableAttribute() {
        updateChangeableAttributeTo(true);
    }


    public DetailedLookupResponse(LookupResponse lookupResponse, League[] leagueDetails, Competition[] competitionDetails) {
        this.setTeams(lookupResponse.getTeams());
        this.setLeagues(lookupResponse.getLeagues());
        this.setCoaches(lookupResponse.getCoaches());
        this.setCompetitions(lookupResponse.getCompetitions());
        //this.setMeta(lookupResponse.getMeta());
        this.leagueDetails = leagueDetails;
        this.competitionDetails = competitionDetails;
        //this.updateChangeableAttribute();
    }
}
