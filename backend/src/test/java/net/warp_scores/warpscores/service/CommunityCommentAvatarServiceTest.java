package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.CommunityComment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommunityCommentAvatarServiceTest {
    @Test
    void attachesReporterAvatarToAiComments() {
        var users = mock(WarpScoresUserRepository.class);
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var reporters = mock(AiReporterRegistry.class);
        var definition = new AiReporterDefinition();
        var portrait = new AiReporterDefinition.Portrait();
        portrait.setAvatar("/img/portraits/ravbert_small.png");
        definition.setPortrait(portrait);
        when(reporters.find("ravbert-smalnos")).thenReturn(Optional.of(definition));
        when(users.findAllById(List.of(42L))).thenReturn(List.of());
        when(profiles.findByUserIdIn(List.of(42L))).thenReturn(List.of());

        var comment = new CommunityComment();
        comment.setAuthorUserId(42L);
        comment.setAuthorSubject("ai:ravbert-smalnos");

        new CommunityCommentAvatarService(users, profiles, reporters).attach(List.of(comment));

        assertThat(comment.getAuthorAvatarUrl()).isEqualTo("/img/portraits/ravbert_small.png");
    }
}