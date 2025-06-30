package net.warp_scores.warpscores.cyanide.api.requests;

import net.warp_scores.warpscores.cyanide.api.responses.RssResponse;

/*
    {
      "game": "bb3",
      "method": "rss",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/rss\/?key={{apiKey}}",
      "args": [],
      "history": [
        "2015\/12\/01 : BLood Bowl RSS feed"
      ]
    },
 */
public class RssRequest extends ApiRequest<RssRequest, RssResponse> {
    public RssRequest() {
        super("bb/rss", RssRequest.class, RssResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_DAY);
    }
}
