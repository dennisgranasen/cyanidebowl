package net.warp_scores.warpscores.cyanide.api.responses;

public interface EmptyAwareResponse {
    default boolean isEmpty() {
        return false;
    }
}
