package net.warp_scores.warpscores.utils;

public interface FieldHandler<T> {
    void handle(T sourceValue, Object target) throws Exception;
}
