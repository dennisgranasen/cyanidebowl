package net.warp_scores.warpscores.utils;

public interface TypeConverter<S, T> {
    T convert(S source);
}
