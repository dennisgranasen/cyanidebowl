package net.warp_scores.warpscores.identity;

import java.util.Arrays;
import java.util.Objects;

/**
 * Composite identity implementation for MongoDB and URLs.
 */
public class CompositeIdentity implements Identity {
    private final String id;
    private final int opus;
    private final String[] parts;

    public CompositeIdentity(int opus, Object... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        this.opus = opus;
        this.parts = Arrays.stream(parts).map(String::valueOf).toArray(String[]::new);
        this.id = buildId(opus, this.parts);
    }

    private static String buildId(int opus, String[] parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(opus);
        for (String part : parts) {
            sb.append('-').append(part);
        }
        return sb.toString();
    }

    @Override
    public String getId() {
        return id;
    }

    public int getOpus() {
        return opus;
    }

    public String[] getParts() {
        return parts.clone();
    }

    /**
     * Parse a CompositeIdentity from a string id.
     * The id must be in the format: opus-part1-part2-...-partN
     */
    public static CompositeIdentity fromId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String[] tokens = id.split("-");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Invalid composite id: " + id);
        }
        int opus = Integer.parseInt(tokens[0]);
        String[] parts = Arrays.copyOfRange(tokens, 1, tokens.length);
        return new CompositeIdentity(opus, (Object[]) parts);
    }
}