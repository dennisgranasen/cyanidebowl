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
@Document("communityComments")
@CompoundIndex(name = "comment_target_created", def = "{'targetType': 1, 'targetId': 1, 'createdAt': 1}")
public class CommunityComment {
    public enum TargetType { ARTICLE, MATCH, TEAM }
    public enum AuthorContext { EDITOR, HOME_COACH, AWAY_COACH, SPECTATOR, USER }

    @Id
    private String id;
    private TargetType targetType;
    private String targetId;
    private String leagueSystemId;
    private Long authorUserId;
    private GenerationProvenance generation = GenerationProvenance.human();
    private String authorSubject;
    private String authorDisplayName;
    private AuthorContext authorContext;
    private String body;
    private Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;
    private String deletedBySubject;
}
