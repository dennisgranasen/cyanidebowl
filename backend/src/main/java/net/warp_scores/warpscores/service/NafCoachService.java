package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.CacheNames;
import net.warp_scores.warpscores.domain.NafCoachDomainService;
import net.warp_scores.warpscores.model.NafCoach;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static net.warp_scores.warpscores.CacheNames.DOMAIN_NAF_COACH;
import static net.warp_scores.warpscores.domain.NafCoachDomainService.NONE_NAF_COACH;

@RequiredArgsConstructor
@Slf4j
@Service
public class NafCoachService {

    public static final String BB3_AI_COACH_NAME = "ARTIFICIAL_INTELLIGENCE";

    private final NafCoachDomainService nafCoachDomainService;
    private final NafCoachLookupClient nafCoachLookupClient;

    @Cacheable(value = DOMAIN_NAF_COACH, unless = "#result == null")
    public Optional<NafCoach> lookupCoach(String coachName) {
        if (BB3_AI_COACH_NAME.equals(coachName)) {
            return Optional.empty();
        }
        String lowerCaseCoachNameWithoutWhitespaces = coachName.trim().toLowerCase().replaceAll("\\s", "");
        Optional<NafCoach> nafCoach = nafCoachDomainService.findByName(lowerCaseCoachNameWithoutWhitespaces);
        if (nafCoach.isEmpty()) {
            nafCoach = nafLookupCoach(lowerCaseCoachNameWithoutWhitespaces);
        }
        return nafCoach;
    }

    private Optional<NafCoach> nafLookupCoach(String lowerCaseCoachNameWithoutWhitespaces) {
        NafCoach nafCoach = nafCoachLookupClient.lookupNafCoach(lowerCaseCoachNameWithoutWhitespaces);
        if (!NONE_NAF_COACH.equals(nafCoach)) {
            nafCoach = nafCoachDomainService.store(nafCoach);
        }
        return Optional.ofNullable(nafCoach);
    }
}


