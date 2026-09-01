package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.controller.LeagueSystemDiscoveryCandidate;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.Platform;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LeagueSystemDiscoveryService {
    private static final Pattern NUMBER = Pattern.compile("(?<!\\d)(\\d{1,3})(?!\\d)");
    private static final Pattern ROMAN = Pattern.compile("(?i)(?<![A-Z])([IVXLCDM]{2,})(?![A-Z])");

    private final MatchRepository matches;
    private final StageSourceRepository stageSources;

    public List<LeagueSystemDiscoveryCandidate> discover(LeagueSystem leagueSystem) {
        List<String> aliases = leagueSystem.getDiscoveryAliases() == null ? List.of() : leagueSystem.getDiscoveryAliases().stream()
                .filter(alias -> alias != null && !alias.isBlank())
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .toList();
        if (aliases.isEmpty()) {
            return List.of();
        }
        Set<String> configuredIds = stageSources.findByLeagueSystemId(leagueSystem.getId()).stream()
                .filter(source -> source.getSourceEntityId() != null)
                .map(source -> source.getSourceEntityId().asMongoKey())
                .collect(java.util.stream.Collectors.toSet());

        return matches.findSourceDiscoveryRecords().stream()
                .filter(record -> matchesAlias(record.leagueName(), record.competitionName(), aliases))
                .filter(record -> record.competitionId() != null)
                .filter(record -> !configuredIds.contains(record.competitionId().asMongoKey()))
                .map(this::candidate)
                .sorted(Comparator.comparing(LeagueSystemDiscoveryCandidate::latestMatch,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean matchesAlias(String leagueName, String competitionName, List<String> aliases) {
        String names = ((leagueName == null ? "" : leagueName) + " "
                + (competitionName == null ? "" : competitionName)).toLowerCase(Locale.ROOT);
        return aliases.stream().anyMatch(names::contains);
    }

    private LeagueSystemDiscoveryCandidate candidate(MatchRepository.SourceDiscoveryRecord record) {
        String sourceId = record.competitionId().asMongoKey();
        return new LeagueSystemDiscoveryCandidate(
                "Competition:" + sourceId,
                sourceId,
                EntityType.Competition,
                record.leagueName(),
                record.competitionName(),
                suggestedSeason(record.competitionName(), record.leagueName()),
                game(record.competitionId().getOpus()),
                platform(record.platform()),
                record.latestMatch(),
                record.matchCount());
    }

    Integer suggestedSeason(String... names) {
        for (String name : names) {
            Integer number = lastArabicNumber(name);
            if (number != null) return number;
            number = lastRomanNumber(name);
            if (number != null) return number;
        }
        return null;
    }

    private Integer lastArabicNumber(String name) {
        if (name == null) return null;
        Matcher matcher = NUMBER.matcher(name);
        Integer result = null;
        while (matcher.find()) result = Integer.valueOf(matcher.group(1));
        return result;
    }

    private Integer lastRomanNumber(String name) {
        if (name == null) return null;
        Matcher matcher = ROMAN.matcher(name.toUpperCase(Locale.ROOT));
        Integer result = null;
        while (matcher.find()) result = romanValue(matcher.group(1));
        return result;
    }

    private int romanValue(String roman) {
        int result = 0;
        int previous = 0;
        for (int index = roman.length() - 1; index >= 0; index--) {
            int current = "IVXLCDM".indexOf(roman.charAt(index));
            int value = new int[]{1, 5, 10, 50, 100, 500, 1000}[current];
            result += value < previous ? -value : value;
            previous = value;
        }
        return result;
    }

    private GameType game(int opus) {
        return switch (opus) {
            case 1 -> GameType.BB1;
            case 2 -> GameType.BB2;
            case 3 -> GameType.BB3;
            default -> null;
        };
    }

    private Platform platform(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Platform.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
