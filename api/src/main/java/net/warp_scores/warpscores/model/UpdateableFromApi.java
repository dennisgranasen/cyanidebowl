package net.warp_scores.warpscores.model;

public interface UpdateableFromApi {
    default boolean isUpdateableFromApi() {
        return true;
    }
}
