package net.warp_scores.warpscores.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NafCoachLookupClient {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NafCoach {
        private String naf_name;
        private Integer naf_id;
        private String error;
    }

    public NafCoach lookupNafCoach(String name) {
        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<NafCoach> nafCoachResult = new ParameterizedTypeReference<>() {};
        ResponseEntity<NafCoach> nafCoachResponse = restTemplate.exchange(
                String.format("https://www.thenaf.net/coachcheck.php?naf_name=%s", name), HttpMethod.GET, null,
                nafCoachResult);
        return nafCoachResponse.getBody();
    }
}
