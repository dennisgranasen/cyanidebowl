package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class AiCompletedMatchFanWorkProducerTest {
    @Test
    void finalizedMatchQueuesOneStableCandidatePerLeagueSystem() {
        StageSourceRepository stageSources = mock(StageSourceRepository.class);
        AiCommunityMemberProfileRepository profiles =
                mock(AiCommunityMemberProfileRepository.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiCompletedMatchFanWorkProducer producer =
                new AiCompletedMatchFanWorkProducer(stageSources, profiles, queue);

        Identity competitionId = mock(Identity.class);
        Match match = match(competitionId, "match-key", "team-a");

        StageSource first = new StageSource();
        first.setLeagueSystemId("league-1");
        StageSource duplicateMapping = new StageSource();
        duplicateMapping.setLeagueSystemId("league-1");
        when(stageSources.findBySourceEntityId(competitionId))
                .thenReturn(List.of(first, duplicateMapping));

        AiCommunityMemberProfile fan = fan("fan-1", "team-a", 1);
        when(profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc())
                .thenReturn(List.of(fan));

        producer.onMatchFinalized(match);

        var captor = forClass(AiAutonomousWorkQueue.EnqueueRequest.class);
        verify(queue).enqueue(captor.capture());

        var request = captor.getValue();
        assertThat(request.candidateKey())
                .isEqualTo("fan-match-comment:league-1:match-key");
        assertThat(request.handlerKey())
                .isEqualTo(AiCompletedMatchFanWorkProducer.HANDLER_KEY);
        assertThat(request.kind())
                .isEqualTo(AiAutonomousWorkItem.WorkKind.FAN_MATCH_COMMENT);
        assertThat(request.priority())
                .isEqualTo(AiAutonomousWorkItem.Priority.AUTONOMOUS);
        assertThat(request.actorId()).isEqualTo("fan-1");
        assertThat(request.targetType()).isEqualTo("MATCH");
        assertThat(request.targetId()).isEqualTo("match-key");
        assertThat(request.maxAttempts()).isEqualTo(1);
    }

    @Test
    void nonFinalizedMatchDoesNotQueue() {
        StageSourceRepository stageSources = mock(StageSourceRepository.class);
        AiCommunityMemberProfileRepository profiles =
                mock(AiCommunityMemberProfileRepository.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiCompletedMatchFanWorkProducer producer =
                new AiCompletedMatchFanWorkProducer(stageSources, profiles, queue);

        Match match = mock(Match.class);
        when(match.getIsFinalized()).thenReturn(false);

        producer.onMatchFinalized(match);

        verifyNoInteractions(stageSources, profiles, queue);
    }

    @Test
    void noParticipatingActiveFanDoesNotQueue() {
        StageSourceRepository stageSources = mock(StageSourceRepository.class);
        AiCommunityMemberProfileRepository profiles =
                mock(AiCommunityMemberProfileRepository.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiCompletedMatchFanWorkProducer producer =
                new AiCompletedMatchFanWorkProducer(stageSources, profiles, queue);

        Identity competitionId = mock(Identity.class);
        Match match = match(competitionId, "match-key", "team-a");
        when(profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc())
                .thenReturn(List.of(fan("fan-x", "other-team", 1)));

        producer.onMatchFinalized(match);

        verifyNoInteractions(stageSources);
        verifyNoInteractions(queue);
    }

    @Test
    void deterministicSelectionIsStable() {
        AiCommunityMemberProfile first = fan("fan-1", "team-a", 1);
        AiCommunityMemberProfile second = fan("fan-2", "team-b", 1);
        List<AiCommunityMemberProfile> fans = List.of(first, second);

        AiCommunityMemberProfile a =
                AiCompletedMatchFanWorkProducer.selectDeterministically(
                        "league", "match", fans);
        AiCommunityMemberProfile b =
                AiCompletedMatchFanWorkProducer.selectDeterministically(
                        "league", "match", fans);

        assertThat(a.getId()).isEqualTo(b.getId());
    }

    private static Match match(
            Identity competitionId,
            String matchKey,
            String teamId) {
        Match match = mock(Match.class);
        Identity matchIdentity = mock(Identity.class);
        when(matchIdentity.asMongoKey()).thenReturn(matchKey);
        when(match.getId()).thenReturn(matchIdentity);
        when(match.getIsFinalized()).thenReturn(true);
        when(match.getCompetitionId()).thenReturn(competitionId);

        Team team = mock(Team.class);
        Identity teamIdentity = mock(Identity.class);
        when(teamIdentity.asMongoKey()).thenReturn(teamId);
        when(team.getId()).thenReturn(teamIdentity);
        when(match.getTeams()).thenReturn(new Team[] { team });

        return match;
    }

    private static AiCommunityMemberProfile fan(
            String id,
            String teamId,
            int ordinal) {
        AiCommunityMemberProfile fan = new AiCommunityMemberProfile();
        fan.setId(id);
        fan.setTeamId(teamId);
        fan.setOrdinal(ordinal);
        fan.setUserId((long) ordinal);
        fan.setUserSubject("ai:community:" + teamId + ":" + ordinal);
        fan.setActive(true);
        return fan;
    }
}
