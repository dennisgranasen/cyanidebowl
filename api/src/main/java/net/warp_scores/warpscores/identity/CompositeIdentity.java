package net.warp_scores.warpscores.identity;

import java.util.Arrays;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
    
/**
 * Composite identity implementation for MongoDB and URLs.
 */
@Getter
@ToString(of = {"opus", "parts"})
@EqualsAndHashCode(of = {"opus", "parts"})
public class CompositeIdentity implements Identity {
    //private String id;
    private int opus;
    private String[] parts;

    public String getKey() {
        return asMongoKey();
    }

    public String getValue() {
        return String.join(DELIMITER, parts);
    }

    public CompositeIdentity() {
    }

    public CompositeIdentity(int opus, String[] parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        this.opus = opus;
        this.parts = parts.clone();
    }

    public CompositeIdentity(int opus, Object... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        this.opus = opus;
        this.parts = Arrays.stream(parts).map(String::valueOf).toArray(String[]::new);
    }

    private static String buildId(int opus, String[] parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(opus);
        for (String part : parts) {
            sb.append(DELIMITER).append(part);
        }
        return sb.toString().toLowerCase();
    }

    public String[] getParts() {
        return parts.clone();
    }

    public String asMongoKey() {
        return buildId(opus, parts);
    }   
    /**
     * Parse a CompositeIdentity from a string id.
     * The id must be in the format: opus-part1-part2-...-partN
     */
    public static CompositeIdentity fromId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String[] tokens = id.split(DELIMITER);
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Invalid composite id: " + id);
        }
        int opus = Integer.parseInt(tokens[0]);
        String[] parts = Arrays.copyOfRange(tokens, 1, tokens.length);
        return new CompositeIdentity(opus, (Object[]) parts);
    }

    public SimpleIdentity asSimpleIdentity(int partIndex){
        if (partIndex < 0 || partIndex >= parts.length) {
            throw new IndexOutOfBoundsException("Part index out of bounds: " + partIndex);
        }
        return new SimpleIdentity(parts[partIndex], opus);   
    }

    @Override
    public int compareTo(Identity other) {
        if (other == null) {
            return 1; // null is considered less than any non-null identity
        }
        return this.asMongoKey().compareTo(other.asMongoKey());
    }
}