package net.warp_scores.warpscores.cyanide.api.model.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObfuscateApiKeyService {

    private final ObjectMapper objectMapper;

    public String obfuscateKey(String value) {
        return value != null ? value.replaceAll("key=[a-zA-Z0-9]+", "key={{apiKey}}") : null;
    }

    public Object obfuscateKey(Object jsonObject) throws JsonProcessingException {
        String jsonAsString = objectMapper.writeValueAsString(jsonObject);
        jsonAsString = obfuscateKey(jsonAsString);
        return objectMapper.readValue(jsonAsString, Object.class);
    }
}
