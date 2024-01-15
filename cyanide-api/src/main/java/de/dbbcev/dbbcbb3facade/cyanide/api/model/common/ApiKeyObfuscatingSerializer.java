package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ApiKeyObfuscatingSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(obfuscateKey(value));
    }

    public static String obfuscateKey(String value) {
        return value != null ? value.replaceAll("key=[a-zA-Z0-9]+", "key={{apiKey}}") : null;
    }
}
