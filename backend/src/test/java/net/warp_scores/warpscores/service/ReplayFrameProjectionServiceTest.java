package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReplayFrameProjectionServiceTest {
    @Test
    void projectsBoardCheckpointsIntoTacticalFrames() throws Exception {
        var artifacts = mock(ReplayArtifactService.class);
        var matches = mock(MatchRepository.class);
        when(artifacts.readCompactJson("match")).thenReturn(("""
                {"steps":[{"sequence":4,"clock":12,"checkpoint":{"reason":"TURN_OR_PHASE_CHANGE","context":{"teams":[{"teamId":0,"gameTurn":3}]},"boardState":{"Ball":{"Cell":{"X":8,"Y":4}},"ListTeams":{"TeamState":[{"Data":{"TeamId":0},"ListPitchPlayers":{"PlayerState":[{"Id":7,"Cell":{"X":8,"Y":4},"LastCell":{"X":7,"Y":4},"Data":{"Name":"QmxpdHplcg==","Number":2,"IdPlayerTypes":1079}},{"Id":8,"Cell":{"X":9,"Y":4},"Data":{"Name":"UnVubmVy","Number":3,"IdPlayerTypes":1080}}]}}]}}}},{"sequence":5,"clock":13,"frame":{"boardState":{"ListTeams":{"TeamState":[{"Data":{"TeamId":0},"ListPitchPlayers":{"PlayerState":[{"Id":7,"Cell":{"X":8,"Y":4},"Data":{}},{"Id":8,"Cell":{"X":9,"Y":4},"Data":{}}]}}]}}}}]}"""
                ).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> projected = new ReplayFrameProjectionService(artifacts, matches, new ObjectMapper()).frames("match");

        assertThat(projected).containsEntry("width", 26).containsEntry("height", 15);
        var frames = (java.util.List<Map<String, Object>>) projected.get("frames");
        assertThat(frames).hasSize(2);
        Map<String, Object> firstFrame = frames.get(0);
        assertThat(firstFrame).containsEntry("sequence", 4).containsEntry("ball", Map.of("x", 8, "y", 4));
        var players = (java.util.List<Map<String, Object>>) firstFrame.get("players");
        assertThat(players).filteredOn(player -> player.get("number").equals(2)).singleElement().satisfies(player -> {
            assertThat(player.get("name")).isEqualTo("Blitzer");
            assertThat(player.get("hasBall")).isEqualTo(true);
            assertThat(player.get("playerTypeId")).isEqualTo(1079);
            assertThat(player.get("positionType")).isEqualTo("Blitzer");
        });
        assertThat(players).filteredOn(player -> player.get("number").equals(3))
            .singleElement().extracting(player -> player.get("positionType")).isEqualTo("Gutter Runner");
        var sparseFramePlayers = (java.util.List<Map<String, Object>>) frames.get(1).get("players");
        assertThat(sparseFramePlayers).extracting(player -> player.get("positionType"))
            .containsExactly("Blitzer", "Gutter Runner");
    }
}