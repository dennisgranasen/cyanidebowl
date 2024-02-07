package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApiTeamTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ApiTeam apiTeam;

    @BeforeEach
    public void setTimeZone() {
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(JsonFormat.DEFAULT_TIMEZONE));
    }

    @Test
    public void team1() throws IOException, ParseException {
        whenDeserialized("testFiles/team1.json");

        assertThat(apiTeam.getId(), is("b6a8f215-99f6-11ee-a745-02000090a64f"));
        assertThat(apiTeam.getName(), is("A Team name"));
        assertThat(apiTeam.getRace(), is(Race.orc));
        assertThat(apiTeam.getLogo(), is("Logo_Orc_12"));
        assertThat(apiTeam.getCoachId(), is(UUID.fromString("b6a8f215-99f6-11ee-a745-02000090a123")));
        assertThat(apiTeam.getLeagueId(), is(UUID.fromString("b6a8f215-99f6-11ee-a745-02000090a987")));
        assertThat(apiTeam.getLeagueName(), is("A league name"));
        assertThat(apiTeam.getCompetitionName(), is("A Competition name"));
        assertThat(apiTeam.getCash(), is(40000));
        assertThat(apiTeam.getValue(), is(BigDecimal.valueOf(1000)));
        assertThat(apiTeam.getCreated(), is(simpleDateFormat.parse("2023-12-13 20:32:20")));
    }

    @Test
    public void team2() throws ParseException, IOException {
        whenDeserialized("testFiles/team2.json");

        assertThat(apiTeam.getName(), is("[DBBC] Dead Street Boys"));
        assertThat(apiTeam.getId(), is("099a83d4-8a44-11ee-b910-02000090a64f"));
        assertThat(apiTeam.getCoachName(), is("chaoskopp"));
        assertThat(apiTeam.getCoachId(), is(UUID.fromString("da26110b-b166-11ed-80a8-020000a4d571")));
        assertThat(apiTeam.getLogo(), is("Logo_Human_01"));
        assertThat(apiTeam.getRace(), is(Race.human));
        assertThat(apiTeam.getMotto(), is("description"));
        assertThat(apiTeam.getDateLastMatch(), is(simpleDateFormat.parse("2024-01-09 20:22:53")));
        assertThat(apiTeam.getLeagueName(), is("DBBL  BB3"));
        assertThat(apiTeam.getLeagueId(), is("94dd6ae4-83fa-11ee-b910-02000090a64f,7119edf3-a16d-11ee-a745-02000090a64f"));
        assertThat(apiTeam.getCompetitionName(), is("DBBL S1 Division 5"));
        assertThat(apiTeam.getBb3_competition_id(),
                is("058e700e-a04f-11ee-a745-02000090a64f,7119edf3-a16d-11ee-a745-02000090a64f"));
    }

    @Test
    public void team3() throws ParseException, IOException {
        whenDeserialized("testFiles/team3.json");

        assertThat(apiTeam.getId(), is("099a83d4-8a44-11ee-b910-02000090a64f"));
        assertThat(apiTeam.getCoachId(), is(UUID.fromString("da26110b-b166-11ed-80a8-020000a4d571")));
        assertThat(apiTeam.getRace(), is(Race.human));
        assertThat(apiTeam.getName(), is("[DBBC] Dead Street Boys"));
        assertThat(apiTeam.getValue(), is(BigDecimal.valueOf(1130)));
        assertThat(apiTeam.getCash(), is(95000));
        assertThat(apiTeam.getCreated(), is(simpleDateFormat.parse("2023-11-23 21:05:32")));
        assertThat(apiTeam.getLogo(), is("Logo_Human_01"));

    }

    private void whenDeserialized(String name) throws IOException {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        apiTeam = objectMapper.readValue(resourceAsStream, ApiTeam.class);
    }

}
