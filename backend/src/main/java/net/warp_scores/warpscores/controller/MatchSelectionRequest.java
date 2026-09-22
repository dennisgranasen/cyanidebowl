package net.warp_scores.warpscores.controller;

public record MatchSelectionRequest(String id, String registeredSourceId,
        Integer firstIndex, Integer lastIndex, String firstId, String lastId,
        java.util.List<String> includedMatchIds, java.util.List<String> excludedMatchIds,
        Boolean isArchived) {
}
