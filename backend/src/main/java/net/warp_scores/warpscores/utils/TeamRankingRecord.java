package net.warp_scores.warpscores.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.warp_scores.warpscores.identity.Identity;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class TeamRankingRecord implements Comparable<TeamRankingRecord> {
    private Identity teamId;
    private String teamName;
    private String teamLogo;
    private Integer raceId;
    private String coachId;
    private String coachName;
    private int points;
    private int wins;
    private int draws;
    private int losses;
    private int netTouchdowns;
    private int netCasualties;
    private int matchCount;
    private String latestTeamValue;
    private int totalTouchdownsFor;
    private int totalTouchdownsAgainst;
    private int totalCasualtiesFor;
    private int totalCasualtiesAgainst;
    // Getters and setters for all fields

    public void add(TeamRankingRecord other) {
        this.points += other.points;
        this.wins += other.wins;
        this.draws += other.draws;
        this.losses += other.losses;
        this.netTouchdowns += other.netTouchdowns;
        this.netCasualties += other.netCasualties;
        this.matchCount += other.matchCount;
        this.latestTeamValue = other.latestTeamValue; // assume latest value is from the most recent record
        this.totalTouchdownsFor += other.totalTouchdownsFor;
        this.totalTouchdownsAgainst += other.totalTouchdownsAgainst;
        this.totalCasualtiesFor += other.totalCasualtiesFor;
        this.totalCasualtiesAgainst += other.totalCasualtiesAgainst;
    }


    @Override
    public int compareTo(TeamRankingRecord other) {
        // Default comparison by points, then net touchdowns, then net casualties,
        // then most touchdowns for, then most casualties for
        int result = Integer.compare(other.points, this.points);
        if (result == 0) {
            result = Integer.compare(other.netTouchdowns, this.netTouchdowns);
        }
        if (result == 0) {
            result = Integer.compare(other.netCasualties, this.netCasualties);
        }
        if (result == 0) {
            result = Integer.compare(other.totalTouchdownsFor, this.totalTouchdownsFor);
        }
        if (result == 0) {
            result = Integer.compare(other.totalCasualtiesFor, this.totalCasualtiesFor);
        }
        return result;
    }
}
