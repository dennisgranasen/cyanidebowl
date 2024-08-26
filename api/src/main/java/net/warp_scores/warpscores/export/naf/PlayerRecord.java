package net.warp_scores.warpscores.export.naf;

import lombok.Getter;
import lombok.Setter;

/*
<![CDATA[
    <playerRecord>
        <name>Bashto</name>
        <number>32021</number>
        <team>Vampire</team>
        <teamRating>100</teamRating>
        <touchDowns>0</touchDowns>
        <badlyHurt>3</badlyHurt>
    </playerRecord>
]]>
*/
@Getter
@Setter
public class PlayerRecord {
    private String name;
    private Integer number;
    private String team;
    private Integer teamRating;
    private Integer touchDowns;
    private Integer badlyHurt;
}
