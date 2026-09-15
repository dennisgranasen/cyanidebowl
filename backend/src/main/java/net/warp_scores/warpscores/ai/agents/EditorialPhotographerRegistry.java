package net.warp_scores.warpscores.ai.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

/** Visual staff are independent of the reporter pool and never receive writing assignments. */
@Component
public class EditorialPhotographerRegistry {
    public record Description(String personality, String focus, String medium, String equipment) {}
    public record Photographer(String id, String alias, Map<String, Description> descriptions) {
        public String imageDirection() {
            var d = descriptions.get("en");
            return "VISUAL AUTHOR: " + alias + "\nPersonality: " + d.personality()
                    + "\nPreferred subjects and composition: " + d.focus()
                    + "\nMedium: " + d.medium() + "\nEquipment and finish: " + d.equipment()
                    + "\nApply this visual signature to any subject, including news, portraits and everyday life."
                    + " Select relevant details from the supplied article and context; never invent match events"
                    + " to satisfy a preference. Equipment quality affects grain, sharpness and texture, not factual accuracy."
                    + " Follow the requested subject while retaining this author's medium and visual personality.";
        }
    }
    private final List<Photographer> photographers;

    public EditorialPhotographerRegistry(ObjectMapper json) throws IOException {
        try (var input = new ClassPathResource("ai/photographers.json").getInputStream()) {
            photographers = List.copyOf(json.readValue(input, new TypeReference<List<Photographer>>() {}));
        }
        var ids = new HashSet<String>();
        for (var p : photographers) {
            if (p.id() == null || p.id().isBlank() || !ids.add(p.id()) || p.alias() == null || p.alias().isBlank()
                    || p.descriptions() == null || !p.descriptions().keySet().containsAll(List.of("sv", "en")))
                throw new IllegalStateException("Invalid photographer profile: " + p.id());
            for (var d : p.descriptions().values()) {
                if (d == null || java.util.stream.Stream.of(d.personality(), d.focus(), d.medium(), d.equipment())
                        .anyMatch(s -> s == null || s.isBlank()))
                    throw new IllegalStateException("Incomplete photographer profile: " + p.id());
            }
        }
        if (photographers.isEmpty()) throw new IllegalStateException("No editorial photographers configured");
    }
    public List<Photographer> all() { return photographers; }
    public Photographer require(String id) {
        // Keep older clients working, while the editor always sends the selected photographer.
        if (id == null || id.isBlank()) return photographers.get(0);
        return photographers.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown photographer"));
    }
}
