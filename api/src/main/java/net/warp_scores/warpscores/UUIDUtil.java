package net.warp_scores.warpscores;

import java.time.Instant;
import java.util.UUID;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;

public class UUIDUtil {
    private static final long NUM_HUNDRED_NANOS_IN_A_SECOND = 10_000_000L;

    private static final long NUM_HUNDRED_NANOS_FROM_UUID_EPOCH_TO_UNIX_EPOCH = 122_192_928_000_000_000L;

    /**
     * Extracts the Instant (with the maximum available 100ns precision) from the given time-based (version 1) UUID.
     *
     * @return the {@link Instant} extracted from the given time-based UUID
     * @throws UnsupportedOperationException If this UUID is not a version 1 UUID
     */
    public static Instant getInstantFromUUID(final UUID uuid) {
        final long hundredNanosSinceUnixEpoch = uuid.timestamp() - NUM_HUNDRED_NANOS_FROM_UUID_EPOCH_TO_UNIX_EPOCH;
        final long secondsSinceUnixEpoch = hundredNanosSinceUnixEpoch / NUM_HUNDRED_NANOS_IN_A_SECOND;
        final long nanoAdjustment = ((hundredNanosSinceUnixEpoch % NUM_HUNDRED_NANOS_IN_A_SECOND) * 100);
        return Instant.ofEpochSecond(secondsSinceUnixEpoch, nanoAdjustment);
    }
    public static UUID getUUIDFromIdentity(final Identity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity must not be null");
        }
        if (identity instanceof net.warp_scores.warpscores.identity.SimpleIdentity) {
            return UUID.fromString(identity.getId().substring(identity.getId().indexOf('-') + 1));
        } else if (identity instanceof net.warp_scores.warpscores.identity.CompositeIdentity) {
            String[] parts = ((net.warp_scores.warpscores.identity.CompositeIdentity) identity).getParts();
            if (parts.length > 0) {
                return UUID.fromString(parts[0]);
            } else {
                throw new IllegalArgumentException("Composite identity must have at least one part");
            }
        } else {
            throw new IllegalArgumentException("Unsupported identity type: " + identity.getClass().getName());
        }
    }
}
