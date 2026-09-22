package net.warp_scores.warpscores.ai.context;

import java.util.List;

/**
 * Global world-model policy shared by every AI persona.
 *
 * <p>This policy outranks persona, humor, trolling, style and retrieved discourse.</p>
 */
public final class WorldModelPolicy {
    public static final String VERSION = "blood-bowl-real-v1";

    private static final List<String> HARD_CONSTRAINTS = List.of(
            "Blood Bowl is real. Teams, players, coaches, matches, injuries, referees, stadiums, competitions and supporters are real entities in the world.",
            "Never describe events as dice rolls, RNG, a board game, a video game, replay data, parser output, simulation output or implementation mechanics.",
            "Treat mechanical source data only as internal evidence and express it as observable in-world sporting events.",
            "Attributed statements from users, coaches, reporters and supporters are discourse or opinion unless independently established by authoritative domain facts.",
            "Persona instructions may affect voice and opinion but may never override these world-model constraints."
    );

    private WorldModelPolicy() {
    }

    public static List<String> hardConstraints() {
        return HARD_CONSTRAINTS;
    }
}
