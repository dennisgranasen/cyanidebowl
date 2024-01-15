package de.dbbcev.dbbcbb3facade.cyanide.api.responses;

public interface EmptyAwareResponse {
    default boolean isEmpty() {
        return false;
    }
}
