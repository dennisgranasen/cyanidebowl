package net.warp_scores.warpscores.service.cyanide;

import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.requests.ContestsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CyanideRestApiClientTest {

    private final CyanideApiProperties cyanideApiProperties = new CyanideApiProperties();

    private final CyanideRestApiClient cyanideRestApiClient = new CyanideRestApiClient(null, cyanideApiProperties, null,
            null);

    @Test
    public void uriIsCorrect() {
        // given
        CyanideApiProperties.ApiConfig apiConfig = new CyanideApiProperties.ApiConfig();
        apiConfig.setBaseUrl("https://web.cyanide-studio.com/ws/");
        cyanideApiProperties.setApiConfig(apiConfig);
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setLimitOffset(42);
        contestsRequest.setLimitSize(23);

        // when
        URI uri = cyanideRestApiClient.createUri(contestsRequest, "apiKey");

        // then
        String expectedUri = "https://web.cyanide-studio.com/ws/bb/contests/?limit=42,+23&key=apiKey";
        assertEquals(expectedUri, uri.toString());
    }
}
