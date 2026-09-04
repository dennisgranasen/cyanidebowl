package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Getter @Setter
@Document(collection="replayDownload")
public class ReplayDownload {
    @Id private String matchId;
    private String gameId;
    /** Legacy path used by downloads created before compressed replay artifacts. */
    private String fileName;
    private String originalFileName;
    private String compactFileName;
    private Long originalSize;
    private Long compactSize;
    private String originalSha256;
    private String originalFormat;
    private String compactSha256;
    private String status;
    private String analysisStatus;
    private Integer parserVersion;
    private Date attemptedAt;
    private Date downloadedAt;
    private Date analyzedAt;
    private String error;
    private String analysisError;
}
