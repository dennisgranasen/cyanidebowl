package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "teams",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/teams\/?key={{apiKey}}",
      "args": {
        "league|league_name": "League name (default = Official League)",
        "league|league_id": "League ID (default : Official League)",
        "competition|competition_name": "Competition name (default = all competitions from given league)",
        "competition|competition_id": "Competition ID (default = all competitions from given league)",
        "platform|platform_name": "pc|playstation|xbox",
        "limit|max": "Max amount of team results (default = 100)",
        "bb|opus": "Opus 1|2|3",
        "sensitive|case_sensitive": "Case-sensitive names matching",
        "race": "Retrieve teams's race info 0|1 (Default: 1)",
        "logo": "Retrieve teams's logo 0|1 (Default: 1)",
        "last_match": "Retrieve teams's last match info 0|1 (Default: 1)"
      },
      "history": [
        "2023\/07\/05 : You can now use OFFSET,LIMIT for the limit parameter (BB3 only)",
        "2023\/06\/22 : BB3 races enumeration format: human | dwarf | skaven | woodElf | etc.",
        "2023\/06\/08 : Add team name, team id and team order parameters",
        "2023\/06\/08 : Add race, logo and match filter parameters",
        "2023\/05\/31 : BB3 Compatibility",
        "2017\/06\/29 : Add dateLastMatch for each team",
        "2015\/12\/01 : List of league\/competition's teams"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamsRequest extends ApiRequest<TeamsRequest, TeamsResponse> {

    private String league_name;
    private UUID league_id;
    private String competition_name;
    private UUID competition_id;
    private Integer case_sensitive;
    private Integer race;
    private Integer logo;
    private Integer last_match;

    public TeamsRequest() {
        super("bb3/teams", TeamsRequest.class, TeamsResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_HOUR);
        setLimitSize(null);
    }
}
