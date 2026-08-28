package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MatchAdapterRegistry {
    private final Map<GameType, MatchAdapter> adapters;

    public MatchAdapterRegistry(List<MatchAdapter> adapters) {
        this.adapters = new EnumMap<>(GameType.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.game(), adapter));
    }

    public MatchAdapter require(GameType game) {
        MatchAdapter adapter = adapters.get(game);
        if (adapter == null) {
            throw new IllegalArgumentException("No match adapter is registered for game " + game);
        }
        return adapter;
    }
}
