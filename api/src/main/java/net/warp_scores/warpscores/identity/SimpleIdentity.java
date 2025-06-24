package net.warp_scores.warpscores.identity;

import java.util.Objects;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Simple identity implementation for MongoDB and URLs.
 */
@Getter
@ToString(of = {"opus", "value"})
@EqualsAndHashCode(of = {"opus", "value"})
public class SimpleIdentity implements Identity {
    private int opus;
    private String value;
    private String key;

    public SimpleIdentity() {
    }

    public SimpleIdentity(Object value, int opus) {
        this.value = Objects.requireNonNull(value, "value must not be null").toString();
        this.opus = opus;
        this.key = asMongoKey();
    }

    public SimpleIdentity(String value, int opus) {
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.opus = opus;
        this.key = asMongoKey();
    }

    public SimpleIdentity(UUID uuid, int opus) {
        this(uuid.toString(), opus);
    }

    public SimpleIdentity(int value, int opus) {
        this(Integer.toString(value), opus);
    }

    public String asMongoKey() {
        return opus + DELIMITER + value;
    }

    /**
     * Parse a SimpleIdentity from a string id in the format opus-value.
     */
    public static SimpleIdentity fromId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String[] tokens = id.split(DELIMITER, 2);
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid simple id: " + id);
        }
        int opus = Integer.parseInt(tokens[0]);
        String value = tokens[1];
        return new SimpleIdentity(value, opus);
    }
}