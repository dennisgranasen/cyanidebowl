package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Getter @Setter
@Document(collection="replaySweeperConfiguration")
public class ReplaySweeperConfiguration {
    @Id private String id="replay-sweeper";
    private boolean enabled=true;
    private String cron="0 0 5 * * *";
    private String zoneId="Europe/Stockholm";
    private int batchSize=10;
    private String steamUsername;
    private boolean credentialConfigured;
    private boolean credentialValid;
    private Date lastStartedAt;
    private Date lastCompletedAt;
    private String lastError;
    private int lastDownloaded;
}
