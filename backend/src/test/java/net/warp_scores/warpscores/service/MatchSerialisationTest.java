package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.service.cyanide.ResponseConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

@ExtendWith(MockitoExtension.class)
public class MatchSerialisationTest {

    private ObjectMapper mapper = new ObjectMapper();

    private ResponseConverter responseConverter = new ResponseConverter(mapper);

    @Mock
    private MatchRepository matchRepository;

    private UUIDConverter uuidConverter = new UUIDConverter();

    private TeamPopulator teamPopulator = new TeamPopulator(uuidConverter);

    private MatchDomainService matchDomainService;

    @BeforeEach
    public  void setUp()
    {
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        matchDomainService = new MatchDomainService(matchRepository, teamPopulator, uuidConverter);
    }

    @Test
    public void convertMatch() throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("MatchResponse.json");

        Object rawResponse = mapper.readValue(inputStream, Object.class);
        System.out.println(rawResponse);
        MatchResponse matchResponse = responseConverter.convertRawResponseToResponseObject(rawResponse, MatchResponse.class);
        ApiMatch apiMatch = matchResponse.getMatch();
        Match targetMatch = new Match();
        matchDomainService.populateMatch(apiMatch, targetMatch);
        System.out.println(targetMatch);
        //  System.out.println(mapper.writeValueAsString(apiMatch));
    }
}
