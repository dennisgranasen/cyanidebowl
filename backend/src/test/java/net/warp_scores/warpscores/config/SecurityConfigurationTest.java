package net.warp_scores.warpscores.config;

import net.warp_scores.warpscores.GlobalErrorHandler;
import net.warp_scores.warpscores.controller.VersionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VersionController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("server")
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GlobalErrorHandler errorHandler;

    @Test
    void debugHeadersAreNotReflected() throws Exception {
        mockMvc.perform(get("/debug-headers")
                        .header("Authorization", "sensitive-test-value")
                        .header("Cookie", "sensitive-test-cookie"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("sensitive-test-value"))))
                .andExpect(content().string(not(containsString("sensitive-test-cookie"))));
    }
}
