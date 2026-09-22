package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Document("aiAutonomousWork")
public class AiAutonomousWorkItem {
    @Id
    private String candidateKey;

    private String handlerKey;
    private WorkKind kind;
    private Priority priority = Priority.AUTONOMOUS;
    private int priorityRank = Priority.AUTONOMOUS.rank();
    private Status status = Status.QUEUED;

    private String leagueSystemId;
    private String actorId;
    private String targetType;
    private String targetId;
    private Map<String, String> payload = new LinkedHashMap<>();

    private int attempts;
    private int maxAttempts = 5;
    private Instant nextAttemptAt;

    private String leaseOwner;
    private Instant leaseUntil;

    private String lastErrorClass;
    private String lastErrorMessage;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public enum WorkKind {
        GENERAL_ARTICLE,
        MATCH_ARTICLE,
        ARTICLE_COMMENT,
        MATCH_COMMENT,
        DIRECT_TAG_REPLY,
        FAN_MATCH_COMMENT,
        CUSTOM
    }

    public enum Priority {
        USER_TRIGGERED(300),
        EDITOR_REQUESTED(200),
        AUTONOMOUS(100);

        private final int rank;

        Priority(int rank) {
            this.rank = rank;
        }

        public int rank() {
            return rank;
        }
    }

    public enum Status {
        QUEUED,
        RUNNING,
        RETRY_WAIT,
        SUCCEEDED,
        FAILED
    }
}
