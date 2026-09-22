package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PyBb3ClientTest {
    @Test
    void unavailableServiceReturns503() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        var client = new PyBb3Client(new ObjectMapper(), "http://localhost:" + port, "test-key");
        assertThatThrownBy(() -> client.get("/health", "test-owner"))
                .isInstanceOfSatisfying(PyBb3ServiceException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(503);
                    assertThat(error.getMessage()).contains("Start the pybb3 service");
                });
    }
}
