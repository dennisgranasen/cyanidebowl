package net.warp_scores.warpscores.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum CompetitionStatus implements Comparable<CompetitionStatus> {
    InProgress, Registration, Finished, Unknown;

    public static CompetitionStatus fromNumber(Integer statusNumber) {
        if (statusNumber == null) return null;
        
        CompetitionStatus[] values = CompetitionStatus.values();
        if (statusNumber >= 0 && statusNumber < values.length) {
            return values[statusNumber];
        }
        throw new IllegalArgumentException("Invalid status number: " + statusNumber);
    }

    public static List<Integer> getStatusNumbers(Collection<CompetitionStatus> statuses) {
        return statuses.stream()
                .map(CompetitionStatus::ordinal)
                .collect(Collectors.toList());
    }
}
