package net.warp_scores.warpscores.utils;

import java.util.HashMap;
import java.util.Map;

public class FieldHandlerRegistry {
    private final Map<String, FieldHandler> handlers = new HashMap<>();

    public void register(String srcField, Class<?> tgtClass, FieldHandler handler) {
        handlers.put(srcField + "->" + tgtClass.getName(), handler);
    }

    public FieldHandler getHandler(String srcField, Class<?> tgtClass) {
        return handlers.get(srcField + "->" + tgtClass.getName());
    }
}