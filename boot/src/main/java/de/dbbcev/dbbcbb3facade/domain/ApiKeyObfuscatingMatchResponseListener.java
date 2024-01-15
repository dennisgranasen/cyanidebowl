package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiMatch;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.MatchResponse;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyObfuscatingMatchResponseListener extends AbstractMongoEventListener<MatchResponse> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<MatchResponse> event) {
        MatchResponse matchResponse = event.getSource();
        Document document = event.getDocument();

        ApiMatch match = matchResponse.getMatch();
        match.setApi_match(ApiKeyObfuscatingSerializer.obfuscateKey(match.getApi_match()));
        document.put("match", match);
    }
}
