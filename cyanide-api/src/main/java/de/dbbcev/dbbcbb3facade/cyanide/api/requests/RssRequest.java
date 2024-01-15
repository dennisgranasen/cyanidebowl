package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;

/*
    {
      "game": "bb3",
      "method": "rss",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/rss\/?key={{api_key}}",
      "args": [],
      "history": [
        "2015\/12\/01 : BLood Bowl RSS feed"
      ]
    },
 */
public class RssRequest extends ApiRequest<RssRequest, RssResponse> {
    public RssRequest() {
        super("bb3/rss", RssRequest.class, RssResponse.class);
    }
}
