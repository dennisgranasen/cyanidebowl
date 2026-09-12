package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.SiteSettingsRepository;
import net.warp_scores.warpscores.model.SiteSettings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocalizationService {
    public static final String FALLBACK_LOCALE = "sv";

    /**
     * The Tolkien languages are intentionally exposed as experimental UI
     * locales. The frontend falls back to English for untranslated messages;
     * we do not fabricate vocabulary for sparsely attested languages.
     */
    public static final List<String> SUPPORTED_LOCALES = List.of(
            "sv", "en", "es", "fi", "pl",
            "sindarin", "quenya", "khuzdul", "entish", "nandorin", "black-speech");

    private static final Set<String> SUPPORTED = Set.copyOf(SUPPORTED_LOCALES);

    private final SiteSettingsRepository repository;

    public SiteSettings settings() {
        return repository.findById(SiteSettings.SITE_ID).orElseGet(SiteSettings::new);
    }

    public String defaultLocale() {
        return normalize(settings().getDefaultLocale(), FALLBACK_LOCALE);
    }

    public SiteSettings updateDefaultLocale(String locale) {
        SiteSettings settings = settings();
        settings.setDefaultLocale(requireSupported(locale));
        return repository.save(settings);
    }

    public String requireSupported(String locale) {
        String normalized = normalize(locale, null);
        if (normalized == null || !SUPPORTED.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }
        return normalized;
    }

    public String optionalSupported(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        return requireSupported(locale);
    }

    private String normalize(String locale, String fallback) {
        if (locale == null || locale.isBlank()) {
            return fallback;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
