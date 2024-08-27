package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.ApplicationProperties;
import net.warp_scores.warpscores.domain.NafCoachDomainService;
import net.warp_scores.warpscores.model.NafCoach;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static net.warp_scores.warpscores.domain.NafCoachDomainService.NONE_NAF_COACH;

@RequiredArgsConstructor
@Slf4j
@Service
public class NafCoachService {

    public static final String BB3_AI_COACH_NAME = "ARTIFICIAL_INTELLIGENCE";

    private final NafCoachDomainService nafCoachDomainService;
    private final NafCoachLookupClient nafCoachLookupClient;

    @CacheEvict(value = "DomainNafCoach", allEntries = true)
    @Scheduled(fixedRateString = "${application.default-spring-cache-ttl}")
    public void emptyCache() {
        log.info("Emptied 'DomainNafCoach' cache.");
    }

    @Cacheable("DomainNafCoach")
    public NafCoach lookupCoach(String coachName) {
        if (BB3_AI_COACH_NAME.equals(coachName)) {
            return NONE_NAF_COACH;
        }
        String lowerCaseCoachNameWithoutWhitespaces = coachName.trim().toLowerCase().replaceAll("\\s", "");
        Optional<NafCoach> nafCoach = nafCoachDomainService.findByName(lowerCaseCoachNameWithoutWhitespaces);
        return nafCoach.orElseGet(() -> nafLookupCoach(lowerCaseCoachNameWithoutWhitespaces));
    }

    private NafCoach nafLookupCoach(String lowerCaseCoachNameWithoutWhitespaces) {
        NafCoach nafCoach = nafCoachLookupClient.lookupNafCoach(lowerCaseCoachNameWithoutWhitespaces);
        if (!NONE_NAF_COACH.equals(nafCoach)) {
            nafCoach = nafCoachDomainService.store(nafCoach);
        }
        return nafCoach;
    }
}


