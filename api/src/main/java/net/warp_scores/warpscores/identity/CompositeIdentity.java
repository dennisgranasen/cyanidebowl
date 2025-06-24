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
    private String key;

    public String getValue() {
        return String.join(DELIMITER, parts);
    }

    public CompositeIdentity() {
    }

    public CompositeIdentity(int opus, Object... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        this.opus = opus;
        this.parts = Arrays.stream(parts).map(String::valueOf).toArray(String[]::new);
        this.key = asMongoKey();
        //this.id = buildId(opus, this.parts);
    }

    private static String buildId(int opus, String[] parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(opus);
        for (String part : parts) {
            sb.append(DELIMITER).append(part);
        }
        return sb.toString();
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
}