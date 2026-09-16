package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.ai.provider.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ArticleImagePromptServiceTest {
    @Test void timeoutInterruptsBlockedTextRequest() throws Exception {
        var interrupted = new java.util.concurrent.CountDownLatch(1);
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                ArticleImagePromptService.bounded(() -> {
                    try { new java.util.concurrent.CountDownLatch(1).await(); }
                    catch (InterruptedException e) { interrupted.countDown(); throw e; }
                    return "unreachable";
                }, java.time.Duration.ofMillis(100)));
        assertEquals(504, error.getStatusCode().value());
        assertTrue(interrupted.await(2, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test void boundsEvenOversizedModelOutputAndPreservesEveryPhotographersStyle() throws Exception {
        var llm = mock(LlmExecutionService.class);
        when(llm.generate(anyString(), any(CanonicalLlmRequest.class))).thenReturn(
                new CanonicalLlmResponse("test", "model", null, "A crowd watching a player. ".repeat(500), null, "stop"));
        var service = new ArticleImagePromptService(llm);
        String source = "Article one. ".repeat(2000) + "Article two. ".repeat(2000) + "Article three. FINAL DETAIL";
        for (var person : new EditorialPhotographerRegistry(new ObjectMapper()).all()) {
            String prompt = service.prepare(source, person, null);
            assertTrue(prompt.length() <= 2048);
            assertTrue(prompt.startsWith(person.imageDirection()));
            assertTrue(prompt.contains(ArticleImagePromptService.BLOOD_BOWL_WORLD));
            assertTrue(prompt.contains("A crowd watching a player."));
            verify(llm).generate(eq(person.id()), argThat(request -> request.taskInstruction().contains(source)));
        }
    }

    @Test void shortGeneralBriefNeedsNoTextCallAndEmptySummaryFailsBeforeImageGeneration() throws Exception {
        var llm = mock(LlmExecutionService.class);
        var service = new ArticleImagePromptService(llm);
        var person = new EditorialPhotographerRegistry(new ObjectMapper()).all().getFirst();
        String shortPrompt = service.prepare("Morg 'n' Thorg bathing in a fountain", person, null);
        assertTrue(shortPrompt.contains("Morg 'n' Thorg bathing in a fountain"));
        assertTrue(shortPrompt.contains(ArticleImagePromptService.BLOOD_BOWL_WORLD));
        assertTrue(shortPrompt.contains("Never depict generic real-world American football or NFL"));
        verifyNoInteractions(llm);
        when(llm.generate(anyString(), any(CanonicalLlmRequest.class))).thenReturn(
                new CanonicalLlmResponse("test", "model", null, "   ", null, "stop"));
        assertThrows(IllegalStateException.class, () -> service.prepare("x".repeat(10000), person, null));
    }
}
