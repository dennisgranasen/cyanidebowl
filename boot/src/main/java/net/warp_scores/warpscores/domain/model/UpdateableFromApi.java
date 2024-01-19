package net.warp_scores.warpscores.domain.model;

public interface UpdateableFromApi {
    default boolean isUpdateableFromApi() {
        return true;
    }
}
