package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiLeague;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.LeaguesResponse;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyObfuscatingLeaguesResponseListener extends AbstractMongoEventListener<LeaguesResponse> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<LeaguesResponse> event) {
        LeaguesResponse leaguesResponse = event.getSource();
        Document document = event.getDocument();

        ApiLeague[] leagues = leaguesResponse.getLeagues();
        for (ApiLeague league : leagues) {
            league.setApi_league(ApiKeyObfuscatingSerializer.obfuscateKey(league.getApi_league()));
        }
        document.put("leagues", leagues);
    }
}
