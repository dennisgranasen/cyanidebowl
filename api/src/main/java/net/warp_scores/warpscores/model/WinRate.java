package net.warp_scores.warpscores.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"wins", "draws", "losses"})
public class WinRate {
    private int wins = 0;
    private int draws = 0;
    private int losses = 0;

    public double getWinRate() {
        int totalGames = wins + losses + draws;
        if (totalGames == 0) {
            return 0;
        }
        return (wins + 0.5 * draws) / (totalGames);
    }

    public static WinRate calculateOverallWinRate(Collection<WinRate> winRates) {
        WinRate overallWinRate = new WinRate();
        winRates.forEach(winRate -> combine().accept(overallWinRate, winRate));
        return overallWinRate;
    }

    private void addWins(int wins) {
        this.wins += wins;
    }

    private void addLosses(int losses) {
        this.losses += losses;
    }

    private void addDraws(int draws) {
        this.draws += draws;
    }

    private static BiConsumer<WinRate, WinRate> combine() {
        return (existingWinRate, addedWinRate) -> {
            existingWinRate.addWins(addedWinRate.getWins());
            existingWinRate.addDraws(addedWinRate.getDraws());
            existingWinRate.addLosses(addedWinRate.getLosses());
        };
    }

    public static BiFunction<WinRate, WinRate, WinRate> merge() {
        return (existingWinRate, addedWinRate) -> {
            WinRate result = new WinRate(
                    existingWinRate.getWins(),
                    existingWinRate.getDraws(),
                    existingWinRate.getLosses()
            );
            combine().accept(result, addedWinRate);
            return result;
        };
    }
}
