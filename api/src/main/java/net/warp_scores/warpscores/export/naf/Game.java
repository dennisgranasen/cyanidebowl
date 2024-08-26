package net.warp_scores.warpscores.export.naf;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/*
<![CDATA[
    <game>
        <timeStamp>2024-08-22 01:00</timeStamp>
        <playerRecord>...</playerRecord>
        <playerRecord>...</playerRecord>
    </game>
]]>
*/
@Getter
@Setter
public class Game {
    private Date timeStamp;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "playerRecord")
    private List<PlayerRecord> playerRecords;
}
