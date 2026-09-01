package net.warp_scores.warpscores.service;

public class StageNotFoundException extends RuntimeException {
    public StageNotFoundException(String stageId) {
        super("Stage not found: " + stageId);
    }
}
