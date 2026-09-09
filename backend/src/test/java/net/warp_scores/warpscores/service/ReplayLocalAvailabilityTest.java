package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.model.ReplayDownload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplayLocalAvailabilityTest {
    @TempDir Path directory;

    @Test
    void readsDockerWindowsAndFilenameRecordsFromConfiguredStorage() throws Exception {
        var downloads = mock(ReplayDownloadRepository.class);
        var artifacts = new ReplayArtifactService(downloads, null, null, null, null);
        ReflectionTestUtils.setField(artifacts, "storageDirectory", directory.toString());
        Files.write(directory.resolve("game.bbr"), new byte[]{1, 2, 3});
        var bytes = new java.io.ByteArrayOutputStream();
        try (var gzip = new java.util.zip.GZIPOutputStream(bytes)) { gzip.write("{}".getBytes()); }
        Files.write(directory.resolve("game.json.gz"), bytes.toByteArray());
        for (String prefix : new String[]{"/app/replays/", "C:\\old\\replays\\", ""}) {
            var record = new ReplayDownload();
            record.setStatus("DOWNLOADED");
            record.setOriginalFileName(prefix + "game.bbr");
            record.setCompactFileName(prefix + "game.json.gz");
            when(downloads.findById("match")).thenReturn(Optional.of(record));
            assertThat(artifacts.originalAvailable(record)).isTrue();
            assertThat(artifacts.compactAvailable(record)).isTrue();
            assertThat(artifacts.readOriginal("match").data()).containsExactly(1, 2, 3);
            assertThat(new String(artifacts.readCompactJson("match"))).isEqualTo("{}");
        }
    }

    @Test
    void restoredLegacyFileBecomesAvailableWithoutChangingHistory() throws Exception {
        var artifacts = new ReplayArtifactService(null, null, null, null, null);
        ReflectionTestUtils.setField(artifacts, "storageDirectory", directory.toString());
        var record = new ReplayDownload();
        record.setStatus("DOWNLOADED");
        record.setFileName(directory.resolve("legacy.xml.gz").toString());
        assertThat(artifacts.originalAvailable(record)).isFalse();
        Files.write(Path.of(record.getFileName()), new byte[]{1});
        assertThat(artifacts.originalAvailable(record)).isTrue();
        assertThat(artifacts.compactAvailable(record)).isFalse();
        assertThat(record.getStatus()).isEqualTo("DOWNLOADED");
    }

    @Test
    void missingFileRejectsAnalysisWithoutOverwritingHistory() {
        var downloads = mock(ReplayDownloadRepository.class);
        var artifacts = mock(ReplayArtifactService.class);
        var record = new ReplayDownload();
        record.setStatus("DOWNLOADED");
        record.setAnalysisStatus("PROCESSED");
        when(downloads.findById("match")).thenReturn(Optional.of(record));
        var service = new ReplayAnalysisBackfillService(downloads, artifacts);
        assertThatThrownBy(() -> service.analyze("match"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        verify(downloads, never()).save(any());
        assertThat(record.getAnalysisStatus()).isEqualTo("PROCESSED");
    }
}
