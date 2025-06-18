package net.warp_scores.warpscores.identity;

/**
 * Utility for recreating Identity instances from id strings.
 */
public class IdentityUtil {
    /**
     * Recreates an Identity (SimpleIdentity or CompositeIdentity) from an id string.
     * If the id contains only one part after opus, it's SimpleIdentity; otherwise, CompositeIdentity.
     */
    public static Identity fromId(String id) {
        if (id == null || !id.contains("-")) {
            throw new IllegalArgumentException("Invalid id format: " + id);
        }
        String[] tokens = id.split("-");
        if (tokens.length == 2) {
            // Format: opus-id
            int opus = Integer.parseInt(tokens[0]);
            String value = tokens[1];
            return new SimpleIdentity(value, opus);
        } else if (tokens.length > 2) {
            // Format: opus-part1-part2-...
            return CompositeIdentity.fromId(id);
        } else {
            throw new IllegalArgumentException("Invalid id format: " + id);
        }
    }
}