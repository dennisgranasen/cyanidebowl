package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.SiteSettings;
import net.warp_scores.warpscores.service.LocalizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequiredArgsConstructor
public class LocalizationController {
    private final LocalizationService localization;

    @GetMapping("/localization")
    public LocalizationView localization() {
        return view(localization.defaultLocale());
    }

    @PutMapping("/admin/localization")
    @PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
    public ResponseEntity<LocalizationView> update(@RequestBody LocalizationUpdate update) {
        try {
            SiteSettings settings = localization.updateDefaultLocale(update.defaultLocale());
            return ResponseEntity.ok(view(settings.getDefaultLocale()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    private LocalizationView view(String defaultLocale) {
        return new LocalizationView(
                defaultLocale,
                LocalizationService.SUPPORTED_LOCALES,
                List.of("sindarin", "quenya", "khuzdul", "entish", "nandorin", "black-speech"));
    }

    public record LocalizationUpdate(String defaultLocale) {}
    public record LocalizationView(String defaultLocale, List<String> supportedLocales,
            List<String> experimentalLocales) {}
}
