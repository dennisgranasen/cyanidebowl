package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static net.warp_scores.warpscores.cyanide.api.responses.TestDateUtil.getDateDaysAgo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchesResponseTest {

    private MatchesResponse givenMatchesResponse;

    @Test
    public void finishedMatchesNotChangeableAfterTenDays() {
        givenMatchesResponse(getDateDaysAgo(20), getDateDaysAgo(20));

        whenUpdateChangeable();

        assertFalse(this.givenMatchesResponse.isChangeableResponse());
    }

    @Test
    public void responseWithOneUnfinishedMatchChangeable() {
        givenMatchesResponse(getDateDaysAgo(20), getDateDaysAgo(20), null);

        whenUpdateChangeable();

        assertTrue(this.givenMatchesResponse.isChangeableResponse());
    }

    @Test
    public void finishedMatchesChangeableWithinTenDays() {
        givenMatchesResponse(getDateDaysAgo(9), getDateDaysAgo(9));

        whenUpdateChangeable();

        assertTrue(this.givenMatchesResponse.isChangeableResponse());
    }

    @Test
    public void unfinishedMatchesChangeable() {
        givenMatchesResponse(null, null);

        whenUpdateChangeable();

        assertTrue(this.givenMatchesResponse.isChangeableResponse());
    }

    private void whenUpdateChangeable() {
        this.givenMatchesResponse.updateChangeableAttribute();
    }

    private void givenMatchesResponse(Date... finishedDates) {
        this.givenMatchesResponse = new MatchesResponse();
        if (finishedDates != null || finishedDates.length > 0) {
            ApiMatch[] matches = Arrays.stream(finishedDates).map(this::newApiMatch).toList()
                    .toArray(new ApiMatch[0]);
            this.givenMatchesResponse.setMatches(matches);
        }
    }

    private ApiMatch newApiMatch(Date finished) {
        ApiMatch apiMatch = new ApiMatch();
        apiMatch.setFinished(finished);
        return apiMatch;
    }
}
