package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentContextMapperTest {
    private final PersistentContextMapper mapper = new PersistentContextMapper();

    @Test
    void memoryAlwaysRemainsAttributedDiscourse() {
        AiMemoryEntry memory = new AiMemoryEntry();
        memory.setId("m-1");
        memory.setOwnerUserId(42L);
        memory.setBody("I have always distrusted this referee.");
        memory.setSubjects(List.of(new SubjectRef(SubjectType.TEAM, "team-1")));
        memory.setSourceContentIds(List.of("comment:c-1"));
        memory.setUpdatedAt(Instant.parse("2026-09-12T07:00:00Z"));

        ContextItem item = mapper.memory(memory);

        assertThat(item.contentType()).isEqualTo(ContextContentType.MEMORY);
        assertThat(item.source()).isEqualTo(ContextSource.MEMORY);
        assertThat(item.authority()).isEqualTo(ContextAuthority.ATTRIBUTED_DISCOURSE);
        assertThat(item.references()).containsExactly("comment:c-1");
    }

    @Test
    void socialRelationshipIsDeterministicDomainFact() {
        AiSocialRelationship relationship = new AiSocialRelationship();
        relationship.setId("42:TEAM_AFFINITY:TEAM:team-1");
        relationship.setUserId(42L);
        relationship.setUserDisplayName("Lady Putridia");
        relationship.setType(AiSocialRelationship.Type.TEAM_AFFINITY);
        relationship.setSubjectType(SubjectType.TEAM);
        relationship.setSubjectId("team-1");
        relationship.setSubjectDisplayName("Råttfällan");

        ContextItem item = mapper.relationship(relationship);

        assertThat(item.contentType()).isEqualTo(ContextContentType.SOCIAL_RELATIONSHIP);
        assertThat(item.source()).isEqualTo(ContextSource.DOMAIN);
        assertThat(item.authority()).isEqualTo(ContextAuthority.DOMAIN_FACT);
        assertThat(item.body()).isEqualTo("Lady Putridia supports Råttfällan.");
    }
}
