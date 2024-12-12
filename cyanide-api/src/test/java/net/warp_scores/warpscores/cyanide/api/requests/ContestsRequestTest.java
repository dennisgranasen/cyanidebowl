package net.warp_scores.warpscores.cyanide.api.requests;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContestsRequestTest {

    @Test
    public void requestUriParams() throws Exception {
        // given
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setLimitOffset(42);
        contestsRequest.setLimitSize(23);

        // when
        MultiValueMap<String, String> queryParams = contestsRequest.toQueryParams();

        //then
        assertTrue(queryParams.containsKey("limit"));
        assertFalse(queryParams.containsKey("limitOffset"));
        assertFalse(queryParams.containsKey("limitSize"));
        assertEquals(List.of("42,+23"), queryParams.get("limit"));
    }
}
