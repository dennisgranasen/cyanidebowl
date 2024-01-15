package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiLeague;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.responses.LeagueResponse;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyObfuscatingLeagueResponseListener extends AbstractMongoEventListener<LeagueResponse> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<LeagueResponse> event) {
        LeagueResponse leagueResponse = event.getSource();
        Document document = event.getDocument();

        ApiLeague league = leagueResponse.getLeague();
        league.setApi_league(ApiKeyObfuscatingSerializer.obfuscateKey(league.getApi_league()));
        document.put("league", league);
    }
}
