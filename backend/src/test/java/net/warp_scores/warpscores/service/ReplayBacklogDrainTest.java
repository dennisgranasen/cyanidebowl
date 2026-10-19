package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.model.ReplayDownload;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Date;
import static org.mockito.Mockito.*;

class ReplayBacklogDrainTest {
    @Test void snapshotUsesBoundedFailureQueryAndDateCounts() {
        var downloads = mock(ReplayDownloadRepository.class);
        var artifacts = mock(ReplayArtifactService.class);
        var worker = new ReplayAnalysisBackfillService(downloads, artifacts);
        when(downloads.findPendingAnalysis(eq(ReplayArtifactService.PARSER_VERSION), any()))
                .thenReturn(List.of());
        when(downloads.countByAnalyzedAtGreaterThanEqual(any(Date.class))).thenReturn(0L);
        when(downloads.findTop10ByAnalysisStatusAndAnalysisErrorIsNotNullOrderByAnalyzedAtDesc("FAILED"))
                .thenReturn(List.of());

        worker.snapshot();

        verify(downloads, times(2)).countByAnalyzedAtGreaterThanEqual(any(Date.class));
        verify(downloads).findTop10ByAnalysisStatusAndAnalysisErrorIsNotNullOrderByAnalyzedAtDesc("FAILED");
        verify(downloads, never()).findAll();
    }

    @Test void consecutivePollsDrainBacklogAndSkipMissingFiles() throws Exception {
        var downloads = mock(ReplayDownloadRepository.class);
        var artifacts = mock(ReplayArtifactService.class);
        var worker = new ReplayAnalysisBackfillService(downloads, artifacts);
        ReflectionTestUtils.setField(worker, "enabled", true);
        var missing = new ReplayDownload(); var first = new ReplayDownload(); var second = new ReplayDownload();
        when(downloads.findPendingAnalysis(eq(ReplayArtifactService.PARSER_VERSION), any()))
                .thenReturn(List.of(missing, first, second), List.of(missing, second), List.of(missing));
        when(artifacts.originalAvailable(first)).thenReturn(true);
        when(artifacts.originalAvailable(second)).thenReturn(true);
        worker.analyzeNewestPending(); worker.analyzeNewestPending(); worker.analyzeNewestPending();
        verify(artifacts).reanalyze(first);
        verify(artifacts).reanalyze(second);
        verify(artifacts, never()).reanalyze(missing);
    }
}
