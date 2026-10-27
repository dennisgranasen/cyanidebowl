package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.reporting.AiPlayerRatingJobService;
import net.warp_scores.warpscores.ai.scheduling.AiCompletedMatchArticleWorkProducer;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.ReplayDownload;
import net.warp_scores.warpscores.model.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplayArtifactServiceTest {
    @TempDir Path directory;

    @Test void reanalysisKeepsSavedStatusOnTheWorkersRecordWithoutReadingItBack() throws Exception {
        var downloads = mock(ReplayDownloadRepository.class);
        var analyses = mock(ReplayAnalysisRepository.class);
        var client = mock(PyBb3Client.class);
        var ratingJobs = mock(AiPlayerRatingJobService.class);
        var matches = mock(MatchRepository.class);
        var articleWork = mock(AiCompletedMatchArticleWorkProducer.class);
        var service = new ReplayArtifactService(downloads, analyses, matches, new ObjectMapper(), client, ratingJobs, articleWork);
        Match match = mock(Match.class);
        when(matches.findById(any())).thenReturn(java.util.Optional.of(match));
        ReflectionTestUtils.setField(service, "storageDirectory", directory.toString());
        var record = record();
        when(client.post(anyString(), anyString(), any())).thenReturn(response(true));

        service.reanalyze(record);

        assertThat(record.getAnalysisStatus()).isEqualTo("PROCESSED");
        assertThat(record.getParserVersion()).isEqualTo(ReplayArtifactService.PARSER_VERSION);
        assertThat(record.getCompactFileName()).isEqualTo("game.json.gz");
        verify(analyses).save(any());
        verify(downloads, times(2)).save(record);
        verify(downloads, never()).findById(anyString());
        verify(ratingJobs).queueAfterReplayAnalysis("3_game");
        verify(articleWork).onMatchFinalized(match);
    }

    @Test void missingAnalysisIsReportedExplicitlyWhileKeepingDownloadedArtifacts() throws Exception {
        var client = mock(PyBb3Client.class);
        var ratingJobs = mock(AiPlayerRatingJobService.class);
        var service = new ReplayArtifactService(mock(ReplayDownloadRepository.class), mock(ReplayAnalysisRepository.class),
            mock(MatchRepository.class), new ObjectMapper(), client, ratingJobs,
            mock(AiCompletedMatchArticleWorkProducer.class));
        ReflectionTestUtils.setField(service, "storageDirectory", directory.toString());
        var record = record();
        when(client.post(anyString(), anyString(), any())).thenReturn(response(false));

        assertThatThrownBy(() -> service.reanalyze(record)).hasMessageContaining("returned no analysis");
        assertThat(record.getStatus()).isEqualTo("DOWNLOADED");
        assertThat(service.compactAvailable(record)).isTrue();
        verifyNoInteractions(ratingJobs);
    }

    private ReplayDownload record() throws Exception {
        Files.write(directory.resolve("game.bbr"), new byte[]{1, 2});
        var record = new ReplayDownload();
        record.setMatchId("3_game"); record.setGameId("game"); record.setOriginalFileName("game.bbr");
        record.setParserVersion(1);
        return record;
    }

    private Map<String, Object> response(boolean analysis) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalData", Base64.getEncoder().encodeToString(new byte[]{1, 2}));
        result.put("compactData", Base64.getEncoder().encodeToString(new byte[]{3, 4}));
        if (analysis) result.put("analysis", Map.of("parserVersion", ReplayArtifactService.PARSER_VERSION));
        return result;
    }
}
