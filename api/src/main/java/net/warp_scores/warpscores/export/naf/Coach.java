package net.warp_scores.warpscores.export.naf;

import lombok.Getter;
import lombok.Setter;

/*
<![CDATA[
    <coach>
        <name>BigMace</name>
        <number>32560</number>
        <team>Chaos Renegade</team>
    </coach>
]]>
 */
@Getter
@Setter
public class Coach {
    private String name;
    private Integer number;
    private String team;
}
