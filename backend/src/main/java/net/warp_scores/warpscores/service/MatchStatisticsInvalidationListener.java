package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Match;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

/** Captures every match persistence path, including imports, backfills and replay discovery. */
@Component
@RequiredArgsConstructor
public class MatchStatisticsInvalidationListener extends AbstractMongoEventListener<Match> {
    private final StatisticsCacheCoordinator statistics;

    @Override
    public void onAfterSave(AfterSaveEvent<Match> event) {
        statistics.matchSaved(event.getSource());
    }
}