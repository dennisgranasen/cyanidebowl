package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporterMemoryConsolidationLlmRequestFactoryTest {
    private final ReporterMemoryConsolidationLlmRequestFactory factory =
            new ReporterMemoryConsolidationLlmRequestFactory(new ObjectMapper());

    @Test
    void parsesDurableMemoryUsingOnlyAllowedSubjects() {
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");

        var result = factory.parse("""
                {
                  "remember": true,
                  "body": "Jag tänker fortsätta hävda att Team One aldrig kan skydda bollen.",
                  "subjectKeys": ["TEAM:team-1"]
                }
                """, List.of(team));

        assertTrue(result.remember());
        assertEquals(List.of(team), result.subjects());
        assertTrue(result.body().contains("Team One"));
    }

    @Test
    void rejectsInventedSubjectKeys() {
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> factory.parse("""
                        {
                          "remember": true,
                          "body": "Ett minne.",
                          "subjectKeys": ["TEAM:team-2"]
                        }
                        """, List.of(team)));

        assertTrue(error.getMessage().contains("unknown subject"));
    }

    @Test
    void rememberFalseProducesNoMemory() {
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m-1");

        var result = factory.parse("""
                {
                  "remember": false,
                  "body": "",
                  "subjectKeys": []
                }
                """, List.of(match));

        assertFalse(result.remember());
        assertNull(result.body());
        assertTrue(result.subjects().isEmpty());
    }
}
