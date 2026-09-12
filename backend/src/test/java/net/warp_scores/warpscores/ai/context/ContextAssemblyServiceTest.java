package net.warp_scores.warpscores.ai.context;

import org.junit.jupiter.api.Test;

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
    void retrievesOnlyCurrentlyImplementedFamiliesAndReturnsCanonicalEnvelope() {
        CanonicalContextRetriever retriever = mock(CanonicalContextRetriever.class);
        when(retriever.currentThread(any(), anyInt())).thenReturn(List.of());
        when(retriever.selfHistory(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());
        when(retriever.discourse(anyLong(), any(Collection.class), anyInt())).thenReturn(List.of());
        when(retriever.domainContext(any(), anyInt())).thenReturn(List.of());

        ContextAssemblyService service = new ContextAssemblyService(retriever, new ContextAssembler());
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m1");
        ContextPlan plan = new ContextPlanner().plan(
                ContextTaskType.MATCH_REPORT, 11L, match, match, List.of());

        AssembledContext result = service.assemble(plan);

        verify(retriever).currentThread(any(), anyInt());
        verify(retriever).selfHistory(anyLong(), any(Collection.class), anyInt());
        verify(retriever).discourse(anyLong(), any(Collection.class), anyInt());
        verify(retriever).domainContext(any(), anyInt());

        assertThat(result.section(ContextSection.SOCIAL)).isEmpty();
        assertThat(result.section(ContextSection.MEMORY)).isEmpty();
    }
}
