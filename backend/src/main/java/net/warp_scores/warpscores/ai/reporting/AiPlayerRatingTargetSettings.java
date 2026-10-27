package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiPlayerRatingTargetSettings {
    private final AiSettingsRepository settings;

    public String editorialTarget() {
        return settings.findById(AiSettings.GLOBAL_ID)
                .map(AiSettings::getEditorialPlayerRatingTarget)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    public String fanTarget() {
        return settings.findById(AiSettings.GLOBAL_ID)
                .map(AiSettings::getFanPlayerRatingTarget)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }
}