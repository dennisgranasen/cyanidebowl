package net.warp_scores.warpscores.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Simple identity implementation for MongoDB and URLs.
 */
public class SimpleIdentity implements Identity {
    private final String id;
    private final int opus;
    private final String value;

    public SimpleIdentity(String value, int opus) {
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.opus = opus;
        this.id = opus + "-" + value;
    }

    public SimpleIdentity(UUID uuid, int opus) {
        this(uuid.toString(), opus);
    }

    public SimpleIdentity(int value, int opus) {
        this(Integer.toString(value), opus);
    }

    @Override
    public String getId() {
        return id;
    }

    public int getOpus() {
        return opus;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a SimpleIdentity from a string id in the format opus-value.
     */
    public static SimpleIdentity fromId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String[] tokens = id.split("-", 2);
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid simple id: " + id);
        }
        int opus = Integer.parseInt(tokens[0]);
        String value = tokens[1];
        return new SimpleIdentity(value, opus);
    }
}