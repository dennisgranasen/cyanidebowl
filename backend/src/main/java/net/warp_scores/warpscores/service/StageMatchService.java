package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchInterpretationRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.stage.AbstractMatchAdapter;
import net.warp_scores.warpscores.domain.stage.ArchiveMatchProvider;
import net.warp_scores.warpscores.domain.stage.MatchAdapterRegistry;
import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchInterpretation;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StageMatchService {
    private static final Comparator<Match> MATCH_ORDER = Comparator
            .comparing(Match::getStarted, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AbstractMatchAdapter::matchKey, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<StageSource> SOURCE_ORDER = Comparator
            .comparing(StageSource::getLegacyEntityIndex, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(StageSource::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final StageRepository stageRepository;
    private final StageSourceRepository stageSourceRepository;
    private final MatchRepository matchRepository;
    private final MatchInterpretationRepository matchInterpretationRepository;
    private final MatchAdapterRegistry matchAdapterRegistry;
    private final List<ArchiveMatchProvider> archiveMatchProviders;

    @Transactional(readOnly = true)
    public List<StageMatchView> getMatchesForStage(String stageId) {
        return getAllMatchesForStage(stageId).stream()
                .filter(view -> view.interpretation() == null
                        || !Boolean.TRUE.equals(view.interpretation().getExcluded()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StageMatchView> getAllMatchesForStage(String stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));

        List<StageSource> sources = stageSourceRepository.findByStageId(stage.getId()).stream()
                .sorted(SOURCE_ORDER)
                .toList();
        if (sources.isEmpty()) {
            return List.of();
        }

        Map<String, SourceMatch> uniqueMatches = new LinkedHashMap<>();
        for (StageSource source : sources) {
            for (Match match : resolveSourceMatches(source)) {
                uniqueMatches.putIfAbsent(identityKey(match), new SourceMatch(source, match));
            }
        }

        List<MatchInterpretation> interpretations = matchInterpretationRepository.findAll();
        return uniqueMatches.values().stream()
                .map(sourceMatch -> adapt(stageId, sourceMatch, interpretations))
                .sorted(Comparator
                        .comparing(StageMatchView::startedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StageMatchView::sourceMatchKey, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Match> resolveSourceMatches(StageSource source) {
        validate(source);
        Map<String, Match> matches = new LinkedHashMap<>();
        consolidatedMatches(source).forEach(match -> matches.put(identityKey(match), match));
        archiveMatchProviders.stream()
                .filter(provider -> provider.supports(source))
                .flatMap(provider -> provider.findMatches(source).stream())
                .forEach(match -> matches.putIfAbsent(identityKey(match), match));
        return applyBoundaries(source, new ArrayList<>(matches.values()));
    }

    private List<Match> consolidatedMatches(StageSource source) {
        return switch (source.getSourceType()) {
            case League -> matchRepository.findByLeagueId(source.getSourceEntityId());
            case Competition -> matchRepository.findByCompetitionId(source.getSourceEntityId());
            default -> throw new IllegalArgumentException(
                    "Unsupported StageSource type " + source.getSourceType() + " for " + source.getId());
        };
    }

    private List<Match> applyBoundaries(StageSource source, List<Match> sourceMatches) {
        List<Match> matches = sourceMatches.stream().sorted(MATCH_ORDER).toList();
        int from = markerIndex(matches, source.getFirstId(), 0, source, "firstId");
        int to = markerIndex(matches, source.getLastId(), matches.size() - 1, source, "lastId") + 1;
        if (from > to) {
            throw new IllegalStateException("Invalid ID boundaries for StageSource " + source.getId());
        }
        matches = matches.subList(from, to);

        int firstIndex = Optional.ofNullable(source.getFirstIndex()).orElse(0);
        int lastIndex = Optional.ofNullable(source.getLastIndex()).orElse(matches.size() - 1);
        if (firstIndex < 0 || lastIndex < firstIndex) {
            throw new IllegalStateException("Invalid index boundaries for StageSource " + source.getId());
        }
        if (matches.isEmpty() || firstIndex >= matches.size()) {
            return List.of();
        }
        return matches.subList(firstIndex, Math.min(lastIndex + 1, matches.size()));
    }

    private int markerIndex(
            List<Match> matches,
            String marker,
            int defaultIndex,
            StageSource source,
            String boundaryName) {
        if (marker == null || marker.isBlank()) {
            return defaultIndex;
        }
        for (int index = 0; index < matches.size(); index++) {
            Match match = matches.get(index);
            if (marker.equals(AbstractMatchAdapter.matchKey(match))
                    || (match.getId() != null && marker.equals(match.getId().asMongoKey()))) {
                return index;
            }
        }
        throw new IllegalStateException(
                boundaryName + " " + marker + " was not found for StageSource " + source.getId());
    }

    private StageMatchView adapt(
            String stageId,
            SourceMatch sourceMatch,
            List<MatchInterpretation> interpretations) {
        MatchInterpretation interpretation = interpretations.stream()
                .filter(candidate -> identifies(candidate, sourceMatch.match()))
                .findFirst()
                .orElse(null);
        return matchAdapterRegistry.require(sourceMatch.source().getGame())
                .adapt(stageId, sourceMatch.source(), sourceMatch.match(), interpretation);
    }

    private boolean identifies(MatchInterpretation interpretation, Match match) {
        return interpretation.identifies(AbstractMatchAdapter.matchKey(match))
                || (match.getId() != null && interpretation.identifies(match.getId().asMongoKey()));
    }

    private void validate(StageSource source) {
        if (source.getSourceEntityId() == null) {
            throw new IllegalArgumentException("StageSource has no sourceEntityId: " + source.getId());
        }
        if (source.getSourceType() == null || source.getSourceType() == EntityType.Circuit) {
            throw new IllegalArgumentException("StageSource has no supported sourceType: " + source.getId());
        }
        if (source.getGame() == null) {
            throw new IllegalArgumentException("StageSource has no game: " + source.getId());
        }
    }

    private String identityKey(Match match) {
        if (match.getId() != null) {
            return match.getId().asMongoKey();
        }
        String matchKey = AbstractMatchAdapter.matchKey(match);
        if (matchKey == null) {
            throw new IllegalArgumentException("Match has neither id nor matchId");
        }
        return matchKey;
    }

    private record SourceMatch(StageSource source, Match match) {
    }
}
