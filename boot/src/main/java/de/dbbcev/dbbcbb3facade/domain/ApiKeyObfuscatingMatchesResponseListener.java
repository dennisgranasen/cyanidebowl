package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiMatch;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.MatchesResponse;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyObfuscatingMatchesResponseListener extends AbstractMongoEventListener<MatchesResponse> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<MatchesResponse> event) {
        MatchesResponse matchesResponse = event.getSource();
        Document document = event.getDocument();

        ApiMatch[] matches = matchesResponse.getMatches();
        for (ApiMatch match : matches) {
            match.setApi_match(ApiKeyObfuscatingSerializer.obfuscateKey(match.getApi_match()));
        }
        document.put("matches", matches);
    }
}
