package de.dbbcev.dbbcbb3facade.domain.model;

public interface UpdateableFromApi {
    default boolean isUpdateableFromApi() {
        return true;
    }
}
