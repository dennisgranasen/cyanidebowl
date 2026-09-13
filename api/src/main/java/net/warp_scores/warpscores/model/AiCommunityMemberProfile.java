package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persistent metadata for an AI-backed community member.
 *
 * <p>The canonical identity is {@link WarpScoresUser}; this document only records
 * community role, team affinity and lifecycle state. Deactivation never deletes the
 * canonical user or authored history.</p>
 */
@Getter
@Setter
@Document("aiCommunityMembers")
@CompoundIndex(
        name = "community_member_team_ordinal",
        def = "{'teamId':1,'ordinal':1}",
        unique = true)
@CompoundIndex(
        name = "community_member_team_active",
        def = "{'teamId':1,'active':1,'ordinal':1}")
public class AiCommunityMemberProfile {
    public enum Role {
        COMMUNITY_MEMBER
    }

    @Id
    private String id;
    private Long userId;
    private String userSubject;
    private Role role = Role.COMMUNITY_MEMBER;

    /** Canonical Team identity encoded with Identity.asMongoKey(). */
    private String teamId;
    private String teamName;
    private String teamRace;

    /** Current slot within the supported team's fan population. */
    private int ordinal;
    private String displayName;

    /** Deterministic provider-independent behavioural seed. */
    private String personaKey;

    /** Public, lightweight community-profile fields. */
    private String species;
    private String bio;
    private String location;
    private String occupation;
    private String favoriteFood;
    private String favoriteDrink;
    private String favoriteChant;

    /** Broad supporter identity, e.g. pub regular, family supporter or former player. */
    private String supporterArchetype;
    /** Friendly human-readable color hint derived from team data when available. */
    private String teamColors;
    /** Stable visual identity shared by avatar and contextual profile photos. */
    private String appearanceBrief;
    /** Prompt/brief for a more social-media like profile photo. */
    private String profileImagePrompt;
    /** Prompt/brief for a tighter avatar portrait. */
    private String avatarPrompt;
    /** Generated or admin-supplied profile-photo URL. */
    private String profileImageUrl;
    /** Generated or admin-supplied avatar URL. */
    private String avatarImageUrl;

    /** Behavioural dimensions in [0,1]. */
    private double optimism = 0.65;
    private double coachPatience = 0.55;
    private double playerPatience = 0.60;
    private double tacticalInterest = 0.50;
    private double matchFocus = 0.65;
    private double foodDrinkInterest = 0.25;
    private double chantInterest = 0.35;
    private double trashTalk = 0.25;
    private double superstition = 0.20;

    /** Loyalty history; canonical user identity never changes. */
    private String originalTeamId;
    private String previousTeamId;
    private Instant loyaltyChangedAt;

    private boolean active = true;
    private Instant createdAt;
    private Instant activatedAt;
    private Instant deactivatedAt;
}
