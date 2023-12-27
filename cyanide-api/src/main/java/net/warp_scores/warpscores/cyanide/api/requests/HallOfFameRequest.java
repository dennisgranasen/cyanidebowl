package net.warp_scores.warpscores.cyanide.api.requests;

/*
    {
      "game": "bb3",
      "method": "halloffame",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/halloffame\/?key={{apiKey}}",
      "args": {
        "bb|opus": "Opus 1|2",
        "platform|platform_name": "pc|playstation|xbox",
        "competition|competition_name": "Competition name (default = all competitions from given league)",
        "league|league_name": "League name (default = Official League)",
        "limit|max": "Max amount of results (default = 100)",
        "exact": "Exact league name match 0|1"
      },
      "history": [
        "2023\/05\/30 : Not compatible with BB3",
        "2015\/12\/01 : League\/competition's hall of fame"
      ]
    },
*/
public class HallOfFameRequest {

    public HallOfFameRequest() {
        throw new UnsupportedOperationException("Not compatible with BB3");
    }
}
