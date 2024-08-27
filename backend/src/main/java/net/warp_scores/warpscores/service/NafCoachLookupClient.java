package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.ApplicationProperties;
import net.warp_scores.warpscores.model.NafCoach;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

import static net.warp_scores.warpscores.domain.NafCoachDomainService.NONE_NAF_COACH;

@Service
@RequiredArgsConstructor
@Slf4j
public class NafCoachLookupClient {

    private final ApplicationProperties applicationProperties;

    @CacheEvict(value = "RestNafCoach", allEntries = true)
    @Scheduled(fixedRateString = "${application.default-spring-cache-ttl}")
    public void emptyCache() {
        log.info("Emptied 'RestNafCoach' cache.");
    }

    @Cacheable("RestNafCoach")
    public NafCoach lookupNafCoach(String name) {
        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<NafCoach> nafCoachResult = new ParameterizedTypeReference<>() {};
        ResponseEntity<NafCoach> nafCoachResponse = restTemplate.exchange(
                String.format(applicationProperties.getNafCoachLookupUrlTemplate(), name), HttpMethod.GET, null,
                nafCoachResult);
        NafCoach nafCoach = nafCoachResponse.getBody();
        if (StringUtils.hasText(nafCoach.getError()) || nafCoach.getNafId() == null) {
            nafCoach = NONE_NAF_COACH;
        }
        log.info("Loaded {} for '{}' from naf.", nafCoach, name);
        return nafCoach;
    }
}
