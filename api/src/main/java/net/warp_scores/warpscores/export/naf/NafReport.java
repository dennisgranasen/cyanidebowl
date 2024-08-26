package net.warp_scores.warpscores.export.naf;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/*
<![CDATA[
    <?xml version="1.0" encoding="UTF-8"?>
    <nafReport xmlns:blo="http://www.bloodbowl.net">
        <organiser>me</organiser>
        <coaches>
            <coach>...</coach>
            <coach>...</coach>
            <coach>...</coach>
        </coaches>
        <game>...</game>
        <game>...</game>
        <game>...</game>
        <game>...</game>
    </nafReport>
]]>
*/
@Getter
@Setter
public class NafReport {
    private String organizer;
    private List<Coach> coaches;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "game")
    private List<Game> games;
}
