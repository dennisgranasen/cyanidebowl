package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorialSubjectContextTest {
    @Test void starPlayersHaveCanonicalRulesAndPinnedPortraits() throws Exception {
        var catalog = new StarPlayerCatalog(new ObjectMapper());
        var service = new EditorialSubjectContext(mock(MongoTemplate.class), mock(AiReporterRegistry.class), catalog, new net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry(new ObjectMapper()));
        var morg = catalog.search("Morg").getFirst();
        var links = List.of(new Article.Association(Article.LinkType.STAR_PLAYER, morg.id()));
        var result = service.resolve(links);
        assertTrue(result.text().contains(morg.name()));
        assertTrue(result.text().contains("Ogre"));
        assertTrue(result.text().contains("Blood Bowl"));
        assertTrue(result.referenceImages().getFirst().contains("abd69e2b68635665f7e2c94bdc69a0819bcfd962/docs/bb2025/media/starplayers/"));
        assertTrue(ArticleScopeService.global(links));
        assertThrows(IllegalArgumentException.class, () -> catalog.require("unknown"));
    }
    @Test void fanAppearanceSeasonAndPublicStaffBioAreSharedWithoutPrivateAccountData() throws Exception {
        var mongo = mock(MongoTemplate.class);
        var service = new EditorialSubjectContext(mongo, mock(AiReporterRegistry.class), new StarPlayerCatalog(new ObjectMapper()), new net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry(new ObjectMapper()));
        var fan = new AiCommunityMemberProfile();
        fan.setDisplayName("Grub"); fan.setSpecies("Goblin"); fan.setAppearanceBrief("Red scarf, crooked nose"); fan.setUserId(10L);
        var season = new Season(); season.setName("Winter Cup"); season.setLeagueSystemId("league");
        var league = new LeagueSystem(); league.setName("Northern League");
        var staff = new WarpScoresUser(); staff.setPublicDisplayName("Editor"); staff.setPublicBio("An orc editor"); staff.setEmail("private@example.com");
        when(mongo.findById("fan", AiCommunityMemberProfile.class)).thenReturn(fan);
        when(mongo.findById("season", Season.class)).thenReturn(season);
        when(mongo.findById("league", LeagueSystem.class)).thenReturn(league);
        when(mongo.findById(42L, WarpScoresUser.class)).thenReturn(staff);
        var context = service.resolve(List.of(new Article.Association(Article.LinkType.FAN, "fan"),
                new Article.Association(Article.LinkType.SEASON, "season"), new Article.Association(Article.LinkType.STAFF, "42")));
        assertTrue(context.text().contains("Red scarf, crooked nose"));
        assertTrue(context.text().contains("Winter Cup")); assertTrue(context.text().contains("Northern League"));
        assertTrue(context.text().contains("An orc editor")); assertFalse(context.text().contains("private@example.com"));
        assertEquals(3, context.subjects().size());
    }
}
