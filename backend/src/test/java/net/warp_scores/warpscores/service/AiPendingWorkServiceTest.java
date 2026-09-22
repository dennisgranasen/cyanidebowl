package net.warp_scores.warpscores.service;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AiPendingWorkServiceTest {
    @Test void existingInactiveFansAreReactivatedBeforeGeneratingNewProfiles() {
        var active = new AiCommunityMemberProfile(); active.setActive(true);
        var inactive = new AiCommunityMemberProfile(); inactive.setActive(false);
        var result = AiPendingWorkService.fanProgress(5, List.of(active, inactive));
        assertEquals(3, result.create());
        assertTrue(result.detail().contains("reactivate 1"));
        assertEquals(4, AiPendingWorkService.fanProgress(5, List.of(active)).create());
        assertEquals(0, AiPendingWorkService.fanProgress(0, List.of(active)).create());
        assertTrue(AiPendingWorkService.fanProgress(0, List.of(active)).detail().contains("deactivate 1"));
        assertNull(AiPendingWorkService.fanProgress(null, List.of()).create());
    }
}
