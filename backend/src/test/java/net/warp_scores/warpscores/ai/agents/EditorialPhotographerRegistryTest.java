package net.warp_scores.warpscores.ai.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorialPhotographerRegistryTest {
    @Test void loadsDistinctBilingualVisualStaffWithCrossSubjectDirections() throws Exception {
        var registry = new EditorialPhotographerRegistry(new ObjectMapper());
        assertEquals(18, registry.all().size());
        assertEquals(18, registry.all().stream().map(p -> p.imageDirection()).distinct().count());
        for (var person : registry.all()) {
            assertNotNull(person.descriptions().get("sv"));
            var english = person.descriptions().get("en");
            assertTrue(person.imageDirection().contains(english.equipment()));
            assertTrue(person.imageDirection().contains(english.medium()));
            assertTrue(person.imageDirection().contains("everyday life"));
            assertTrue(person.imageDirection().contains("never invent match events"));
        }
        assertTrue(registry.require("pip-kritsmula").imageDirection().contains("crayon"));
        assertTrue(registry.require("selma-vattenfarg").imageDirection().contains("Watercolor"));
        assertTrue(registry.require("kolgrim-sotfinger").imageDirection().contains("charcoal"));
        assertTrue(registry.require("linus-stift").imageDirection().contains("Pencil"));
    }

    @Test void legacyClientsGetDefaultButUnknownIdsAreRejected() throws Exception {
        var registry = new EditorialPhotographerRegistry(new ObjectMapper());
        assertEquals(registry.all().get(0), registry.require(null));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> registry.require("unknown"));
    }
}
