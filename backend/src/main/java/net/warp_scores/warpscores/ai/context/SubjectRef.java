package net.warp_scores.warpscores.ai.context;

import java.util.Locale;
import java.util.Objects;

/** Stable, provider-neutral reference to something context can be about. */
public record SubjectRef(SubjectType type, String id) {
    public SubjectRef {
        Objects.requireNonNull(type, "type");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("subject id must not be blank");
        }
        id = id.trim();
    }

    public static SubjectRef general() {
        return new SubjectRef(SubjectType.GENERAL, "general");
    }

    public static SubjectRef topic(String topic) {
        return new SubjectRef(SubjectType.TOPIC, topic.trim().toLowerCase(Locale.ROOT));
    }
}
