package net.warp_scores.warpscores.cyanide.api.requests;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchesRequestTest {

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void requestUriParams() throws Exception {
        // given
        MatchesRequest matchesRequest = new MatchesRequest();
        Date start = dateFormat.parse("2024-06-01 23:20:05");
        matchesRequest.setStart(start);

        // when
        MultiValueMap<String, String> queryParams = matchesRequest.toQueryParams();

        // then
        assertTrue(queryParams.containsKey("start"));
        assertEquals(List.of("2024-06-01T23:20:05"), queryParams.get("start"));
    }
}
