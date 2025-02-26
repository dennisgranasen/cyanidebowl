package net.warp_scores.warpscores.model;

public enum MatchStatus {
    Sheduled,
    Scheduled,
    Validated,
    InProgress,
    Played,
    Canceled,
    Calculated,
    @Deprecated
    scheduled(true, Scheduled),
    @Deprecated
    in_progress(true, InProgress),
    @Deprecated
    played(true, Validated);

    private final boolean deprecated;
    private final MatchStatus superseededMatchStatus;

    MatchStatus() {
        this(false, null);
    }

    MatchStatus(boolean deprecated, MatchStatus superseededMatchStatus) {
        this.deprecated = deprecated;
        this.superseededMatchStatus = superseededMatchStatus;
    }

    public MatchStatus undeprecate() {
        if (deprecated) {
            return superseededMatchStatus;
        } else {
            return this;
        }
    }
}
