package net.warp_scores.warpscores.model;

public enum CompetitionFormat implements Comparable<CompetitionFormat> {
    undefined,
    /*BB2*/ round_robin, single_elimination, ladder, swiss,
    /*BB3*/ RoundRobin, Knockout, Wissen, Ladder, Arena;

     /**
     * Returns the canonical format, mapping BB2 formats to BB3 equivalents
     */
    public CompetitionFormat getCanonical() {
        return switch (this) {
            case round_robin -> RoundRobin;
            case single_elimination -> Knockout;
            case ladder -> Ladder;
            case swiss -> Wissen;
            default -> this;
        };
    }
    
    /**
     * Check if this format is equivalent to another (considering aliases)
     */
    public boolean isEquivalentTo(CompetitionFormat other) {
        return this.getCanonical() == other.getCanonical();
    }
}
