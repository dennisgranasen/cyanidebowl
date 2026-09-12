package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LlmPlayerRatingGeneratorTest {
    private final ContextPlanner planner = mock(ContextPlanner.class);
    private final ContextAssemblyService assembly = mock(ContextAssemblyService.class);
    private final LlmExecutionService llm = mock(LlmExecutionService.class);
    private final AiPlayerMatchRatingRepository ratings = mock(AiPlayerMatchRatingRepository.class);

    private final LlmPlayerRatingGenerator generator = new LlmPlayerRatingGenerator(
            new ObjectMapper(), planner, assembly, llm, ratings);

    @Test
    void validatesWholeResponseBeforeWritingAnything() {
        AiReporterDefinition reporter = reporter();
        PlayerRatingFacts facts = facts();

        when(assembly.assemble(isNull())).thenReturn(emptyContext());
        when(llm.generate(eq("r1"), any())).thenReturn(response("""
                {"ratings":[
                  {"playerId":"p1","rating":2,"verdict":"Bra"},
                  {"playerId":"p2","rating":4,"verdict":"Ogiltig"}
                ]}
                """));

        assertThrows(IllegalArgumentException.class,
                () -> generator.generateAndPersist(reporter, facts, null));

        verify(ratings, never()).saveAll(any());
        verify(llm, times(1)).generate(eq("r1"), any());
    }

    @Test
    void persistsEveryPlayerExactlyOnceOnMinusThreeToPlusThreeScale() {
        AiReporterDefinition reporter = reporter();
        PlayerRatingFacts facts = facts();

        when(assembly.assemble(isNull())).thenReturn(emptyContext());
        when(llm.generate(eq("r1"), any())).thenReturn(response("""
                {"ratings":[
                  {"playerId":"p1","rating":-3,"verdict":"Katastrof"},
                  {"playerId":"p2","rating":3,"verdict":"Matchvinnare"}
                ]}
                """));

        generator.generateAndPersist(reporter, facts, "Var tydlig");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AiPlayerMatchRating>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(ratings).saveAll(captor.capture());

        List<AiPlayerMatchRating> saved = new java.util.ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertEquals(2, saved.size());
        assertEquals(List.of("p1", "p2"),
                saved.stream().map(AiPlayerMatchRating::getPlayerId).sorted().toList());
        assertTrue(saved.stream().allMatch(r -> r.getRating() >= -3 && r.getRating() <= 3));
        assertEquals(2, saved.stream().map(AiPlayerMatchRating::getId).distinct().count());
        verify(llm, times(1)).generate(eq("r1"), any());
    }

    @Test
    void rejectsResponseThatOmitsAPlayerWithoutOverwritingExistingRows() {
        when(assembly.assemble(isNull())).thenReturn(emptyContext());
        when(llm.generate(eq("r1"), any())).thenReturn(response("""
                {"ratings":[
                  {"playerId":"p1","rating":1,"verdict":"Bra"}
                ]}
                """));

        assertThrows(IllegalArgumentException.class,
                () -> generator.generateAndPersist(reporter(), facts(), null));

        verify(ratings, never()).saveAll(any());
    }

    private static AiReporterDefinition reporter() {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId("r1");
        reporter.setAlias("Reporter");
        reporter.setUserId(42L);
        return reporter;
    }

    private static PlayerRatingFacts facts() {
        return PlayerRatingFacts.builder()
                .schemaVersion("facts-v1")
                .matchId("match-1")
                .matchSummary(Map.of())
                .players(List.of(
                        PlayerRatingFacts.Player.builder()
                                .playerId("p1").playerName("One").teamId("t1").race("HUMAN").build(),
                        PlayerRatingFacts.Player.builder()
                                .playerId("p2").playerName("Two").teamId("t2").race("ORC").build()))
                .build();
    }

    private static AssembledContext emptyContext() {
        return new AssembledContext("test", List.of(), Map.of(), 0, 0);
    }

    private static CanonicalLlmResponse response(String content) {
        return new CanonicalLlmResponse(
                "test-provider", "test-model", "req-1", content,
                CanonicalLlmResponse.Usage.unknown(), "stop");
    }
}
