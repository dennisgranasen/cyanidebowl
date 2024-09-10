package net.warp_scores.warpscores.service.cyanide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseConverter {

    private final ObjectMapper objectMapper;

    public <ResponseType> ResponseType convertRawResponseToResponseObject(Object rawResponse,
            Class<ResponseType> responseClass) {
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        if (rawResponse == null) {
            return null;
        }
        try {
            ResponseType responseType = objectMapper.readValue(objectMapper.writeValueAsString(rawResponse),
                    responseClass);
            log.debug("Converted raw response to '{}'.", responseType.toString());
            return responseType;
        } catch (JsonProcessingException ex) {
            log.error("Unable to convert raw response {} to response object (type: {})...", rawResponse, responseClass);
            log.error("Exception: ", ex);
            return null;
        }
    }
}
