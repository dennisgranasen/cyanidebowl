package net.warp_scores.warpscores.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityJacksonTest {
    @Test
    void testSimpleIdentitySerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleIdentity id = new SimpleIdentity("abc", 3);
        String json = mapper.writeValueAsString(id);
        Identity deserialized = mapper.readValue(json, Identity.class);
        assertThat(deserialized).isInstanceOf(SimpleIdentity.class);
        assertThat(((SimpleIdentity) deserialized).getValue()).isEqualTo("abc");
        assertThat(((SimpleIdentity) deserialized).getOpus()).isEqualTo(3);
    }
}