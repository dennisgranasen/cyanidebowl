package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.ReplayDownload;
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
        var service = new ReplayArtifactService(downloads, analyses, mock(MatchRepository.class), new ObjectMapper(), client);
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
    }

    @Test void missingAnalysisIsReportedExplicitlyWhileKeepingDownloadedArtifacts() throws Exception {
        var client = mock(PyBb3Client.class);
        var service = new ReplayArtifactService(mock(ReplayDownloadRepository.class), mock(ReplayAnalysisRepository.class),
                mock(MatchRepository.class), new ObjectMapper(), client);
        ReflectionTestUtils.setField(service, "storageDirectory", directory.toString());
        var record = record();
        when(client.post(anyString(), anyString(), any())).thenReturn(response(false));

        assertThatThrownBy(() -> service.reanalyze(record)).hasMessageContaining("returned no analysis");
        assertThat(record.getStatus()).isEqualTo("DOWNLOADED");
        assertThat(service.compactAvailable(record)).isTrue();
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
