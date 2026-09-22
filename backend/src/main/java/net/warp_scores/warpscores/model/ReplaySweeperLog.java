package net.warp_scores.warpscores.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
@Getter @Setter @Document(collection="replaySweeperLog")
public class ReplaySweeperLog {
    @Id private String id;
    @Indexed(expireAfter="90d") private Date createdAt;
    private String level;
    private String code;
    private String message;
    private Integer downloaded;
}
