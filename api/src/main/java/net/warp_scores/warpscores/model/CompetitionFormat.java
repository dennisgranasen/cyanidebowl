package net.warp_scores.warpscores.model;

public enum CompetitionFormat implements Comparable<CompetitionFormat> {
    undefined,
    /*BB2*/ round_robin, single_elimination, ladder, swiss,
    /*BB3*/ RoundRobin, Knockout, Wissen, Ladder, Arena
}
