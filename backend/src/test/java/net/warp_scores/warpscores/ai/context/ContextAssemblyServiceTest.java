package net.warp_scores.warpscores.ai.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.service.StarPlayerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextAssemblyServiceTest {

    @Test
    void resolvesStarPlayerKeysAndPreservesUnknownKeys() throws IOException {
        StarPlayerPromptNormalizer names = new StarPlayerPromptNormalizer(
                new StarPlayerCatalog(new ObjectMapper()));

        assertThat(names.displayName("sp_dribblesnot")).isEqualTo("Bomber Dribblesnot");
        assertThat(names.displayName("name_sp_dribblesnot")).isEqualTo("Bomber Dribblesnot");
        assertThat(names.displayName("PLAYER_NAMES_CHAMPION_DRIBBLESNOT"))
                .isEqualTo("Bomber Dribblesnot");
        assertThat(names.normalizeText("sp_dribblesnot and sp_unknown"))
                .isEqualTo("Bomber Dribblesnot and sp_unknown");
    }

    @Test
        void retrievesAllSixFamiliesAndExpandsSubjectsFromDomain() throws IOException {
        CanonicalContextRetriever retriever = mock(CanonicalContextRetriever.class);
        SocialContextRetriever socialRetriever = mock(SocialContextRetriever.class);
        MemoryContextRetriever memoryRetriever = mock(MemoryContextRetriever.class);

        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m1");
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");
        ContextItem domainItem = new ContextItem(
                "match:m1",
                ContextContentType.DOMAIN_REFERENCE,
                ContextSource.DOMAIN,
                ContextAuthority.DOMAIN_FACT,
                null, null, null,
                Instant.parse("2026-09-12T06:00:00Z"),
                match,
                null,
                List.of(match, team),
                List.of(),
                List.of(),
                "Team A - Team B",
                "sp_dribblesnot scored for Team A.",
                null);

        when(retriever.domainContext(any(), anyInt())).thenReturn(List.of(domainItem));
        when(retriever.currentThread(any(), anyInt())).thenReturn(List.of());
        when(retriever.selfHistory(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());
        when(retriever.discourse(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());
        when(socialRetriever.socialContext(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());
        when(memoryRetriever.memoryContext(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());

        ContextAssemblyService service = new ContextAssemblyService(
                retriever, socialRetriever, memoryRetriever, new ContextAssembler(),
                new StarPlayerPromptNormalizer(new StarPlayerCatalog(new ObjectMapper())));
        ContextPlan plan = new ContextPlanner().plan(
                ContextTaskType.MATCH_REPORT, 11L, match, match, List.of());

        AssembledContext result = service.assemble(plan);

        verify(retriever).currentThread(any(), anyInt());
        verify(retriever).selfHistory(anyLong(), org.mockito.ArgumentMatchers.argThat(
                subjects -> ((Collection<?>) subjects).contains(team)), anyInt());
        verify(retriever).discourse(anyLong(), org.mockito.ArgumentMatchers.argThat(
                subjects -> ((Collection<?>) subjects).contains(team)), anyInt());
        verify(socialRetriever).socialContext(anyLong(), org.mockito.ArgumentMatchers.argThat(
                subjects -> ((Collection<?>) subjects).contains(team)), anyInt());
        verify(memoryRetriever).memoryContext(anyLong(), org.mockito.ArgumentMatchers.argThat(
                subjects -> ((Collection<?>) subjects).contains(team)), anyInt());

        assertThat(result.section(ContextSection.DOMAIN)).singleElement()
                .satisfies(item -> assertThat(item.body()).isEqualTo("Bomber Dribblesnot scored for Team A."));
        assertThat(result.section(ContextSection.SOCIAL)).isEmpty();
        assertThat(result.section(ContextSection.MEMORY)).isEmpty();
    }
}
