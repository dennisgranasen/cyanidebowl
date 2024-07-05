package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static net.warp_scores.warpscores.cyanide.api.responses.TestDateUtil.getDateDaysAgo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchResponseTest {

    private MatchResponse givenMatchResponse;

    @Test
    public void responseWithoutMatchChangeable() {
        givenEmptyMatchResponse();

        whenUpdateChangeable();

        assertTrue(this.givenMatchResponse.isChangeableResponse());
    }

    private void givenEmptyMatchResponse() {
        this.givenMatchResponse = new MatchResponse();
    }

    @Test
    public void finishedMatchNotChangeableAfterTenDays() {
        givenMatchResponse(getDateDaysAgo(20));

        whenUpdateChangeable();

        assertFalse(this.givenMatchResponse.isChangeableResponse());
    }

    @Test
    public void finishedMatchChangeableWithinTenDays() {
        givenMatchResponse(getDateDaysAgo(9));

        whenUpdateChangeable();

        assertTrue(this.givenMatchResponse.isChangeableResponse());
    }

    @Test
    public void unfinishedMatchChangeable() {
        givenMatchResponse(null);

        whenUpdateChangeable();

        assertTrue(this.givenMatchResponse.isChangeableResponse());
    }

    private void whenUpdateChangeable() {
        this.givenMatchResponse.updateChangeableAttribute();
    }

    private void givenMatchResponse(Date finished) {
        ApiMatch match = new ApiMatch();
        match.setFinished(finished);
        this.givenMatchResponse = new MatchResponse();
        this.givenMatchResponse.setMatch(match);
    }
}
