package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ResponseMeta;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.ApiResponse;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyObfuscatingApiResponseListener extends AbstractMongoEventListener<ApiResponse> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<ApiResponse> event) {
        ApiResponse apiResponse = event.getSource();
        Document document = event.getDocument();

        ResponseMeta meta = apiResponse.getMeta();
        meta.setServices(ApiKeyObfuscatingSerializer.obfuscateKey(meta.getServices()));
        document.put("meta", meta);
    }
}
