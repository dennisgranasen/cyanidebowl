package net.warp_scores.warpscores.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@With
@AllArgsConstructor
@NoArgsConstructor
public class Stats {
    private int wins;
    private int losses;
    private int draws;
    private int inflictedTd;
    private int inflictedCas;
    private int sustainedTd;
    private int sustainedCas;
    private int matchCount;

    void accumulate(Stats other) {
        wins += other.wins;
        losses += other.losses;
        draws += other.draws;
        inflictedTd += other.inflictedTd;
        inflictedCas += other.inflictedCas;
        sustainedTd += other.sustainedTd;
        sustainedCas += other.sustainedCas;
        matchCount += other.matchCount;
    }

    public BigDecimal getWinrate() {
        return BigDecimal.valueOf( (wins + 0.5*draws)/ (double) matchCount).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }
}
