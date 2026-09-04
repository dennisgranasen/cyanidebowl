package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.WarpScoresApp;
import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.service.StageMatchService;
import net.warp_scores.warpscores.service.StageNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = WarpScoresApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"AUTH0_URI=https://auth.example.test/", "AUTH0_AUDIENCE=nst-scores-backend"})
@ActiveProfiles("server")
class StageControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private StageMatchService stageMatchService;

    @Test
    void returnsPublicStageMatchDtosAndMapsStageErrors() throws Exception {
        when(stageMatchService.getMatchesForStage("stage-1")).thenReturn(List.of(stageMatch()));
        HttpResponse<String> success = get("stage-1");
        assertThat(success.statusCode()).isEqualTo(200);
        assertThat(success.body()).contains("\"sourceMatchKey\":\"match-1\"");
        assertThat(success.body()).contains("\"matchResourceId\":\"3_match-1\"");
        assertThat(success.body()).doesNotContain("interpretation");

        when(stageMatchService.getMatchesForStage("missing"))
                .thenThrow(new StageNotFoundException("missing"));
        assertThat(get("missing").statusCode()).isEqualTo(404);

        when(stageMatchService.getMatchesForStage("invalid"))
                .thenThrow(new IllegalStateException("Invalid boundaries"));
        assertThat(get("invalid").statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> get(String stageId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/stages/" + stageId + "/matches"))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private StageMatchView stageMatch() {
        return new StageMatchView(
                "stage-1",
                "source-1",
                GameType.BB3,
                Platform.PC,
                new SimpleIdentity("match-1", 3),
                "match-1",
                new SimpleIdentity("competition-1", 3),
                Date.from(Instant.parse("2026-01-01T00:00:00Z")),
                null,
                "finalized",
                "1",
                null,
                null,
                new StageMatchView.Score(1, 0),
                new StageMatchView.Score(1, 0),
                false,
                false,
                false,
                StageMatchView.Quality.COMPLETE,
                new StageMatchView.Capabilities(true, true, false),
                new StageMatchView.CountingRules(true, true, true, true),
                null);
    }
}
