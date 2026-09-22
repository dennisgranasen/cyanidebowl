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
                  "subjectKeys": ["TEAM:team-1"],
                  "relationships": [
                    {
                      "subjectKey": "TEAM:team-1",
                      "sentiment": -0.6,
                      "confidence": 0.8,
                      "rationale": "Reportern uttrycker återkommande förakt för lagets bollskydd."
                    }
                  ]
                }
                """, List.of(team));

        assertTrue(result.remember());
        assertEquals(List.of(team), result.subjects());
        assertTrue(result.body().contains("Team One"));
        assertEquals(1, result.relationships().size());
        assertEquals(team, result.relationships().get(0).subject());
        assertEquals(-0.6, result.relationships().get(0).sentiment(), 0.001);
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
                          "subjectKeys": ["TEAM:team-2"],
                          "relationships": []
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
                  "subjectKeys": [],
                  "relationships": []
                }
                """, List.of(match));

        assertFalse(result.remember());
        assertNull(result.body());
        assertTrue(result.subjects().isEmpty());
        assertTrue(result.relationships().isEmpty());
    }

    @Test
    void rejectsRelationshipForNonTeamOrCoachSubject() {
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m-1");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> factory.parse("""
                        {
                          "remember": false,
                          "body": "",
                          "subjectKeys": [],
                          "relationships": [
                            {
                              "subjectKey": "MATCH:m-1",
                              "sentiment": -0.4,
                              "confidence": 0.7,
                              "rationale": "Fel sorts relationsmål."
                            }
                          ]
                        }
                        """, List.of(match)));

        assertTrue(error.getMessage().contains("TEAM or COACH_IDENTITY"));
    }
}
