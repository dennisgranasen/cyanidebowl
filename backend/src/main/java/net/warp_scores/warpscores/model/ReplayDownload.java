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
    private String fileName;
    private String status;
    private Date attemptedAt;
    private Date downloadedAt;
    private String error;
}
