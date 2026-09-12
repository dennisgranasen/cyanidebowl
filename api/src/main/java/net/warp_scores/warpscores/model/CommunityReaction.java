package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Document("communityReactions")
@CompoundIndex(name = "reaction_user_target_unique",
        def = "{'targetType': 1, 'targetId': 1, 'userSubject': 1}", unique = true)
public class CommunityReaction {
    public enum TargetType { ARTICLE, MATCH_ARTICLE, COMMENT, MATCH, TEAM }
    public enum Type {
        POW(1), DOUBLE_POW(2), TRIPLE_POW(3),
        SKULL(-1), DOUBLE_SKULL(-2), TRIPLE_SKULL(-3);

        private final int weight;
        Type(int weight) { this.weight = weight; }
        public int getWeight() { return weight; }
    }

    @Id
    private String id;
    private TargetType targetType;
    private String targetId;
    private String userSubject;
    private Long userId;
    private Type type;
    private Instant updatedAt;
}
