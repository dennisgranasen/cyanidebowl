package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.NafCoach;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class NafExporterTest {

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMANY);

    private List<Contest> contests = new ArrayList<>();

    private NafReport nafReport;

    @Mock
    private ContestService contestService;

    @Mock
    private NafCoachService nafCoachService;

    private NafXmlCreator nafXmlCreator = new NafXmlCreator();

    @InjectMocks
    private NafExporter nafExporter;

    @BeforeEach
    public void setup() throws IOException {
        Mockito.when(nafCoachService.lookupCoach(eq("HumanCoach")))
                .thenReturn(new NafCoach(
                        42, "HumanCoach", null));
        Mockito.when(nafCoachService.lookupCoach(eq("OrcCoach")))
                .thenReturn(new NafCoach(
                        23, "OrcCoach", null));
    }

    @Test
    public void testExportContests() throws IOException {
        givenContests();

        whenExported();

        thenAssertExportToBe(
                "<?xml version='1.0' encoding='UTF-8'?><nafReport><organiser>warp-scores.net//Test</organiser><coaches><coach><name>HumanCoach</name><number>42</number><team>Human</team></coach><coach><name>OrcCoach</name><number>23</number><team>Orc</team></coach></coaches><game><timeStamp>2024-02-26 14:00</timeStamp><playerRecord><name>HumanCoach</name><number>42</number><team>Human</team><teamRating>100</teamRating><touchDowns>2</touchDowns><badlyHurt>1</badlyHurt></playerRecord><playerRecord><name>OrcCoach</name><number>23</number><team>Orc</team><teamRating>100</teamRating><touchDowns>1</touchDowns><badlyHurt>3</badlyHurt></playerRecord></game></nafReport>");
    }

    private void whenExported() {
        nafReport = nafExporter.export(contests, "Test");
    }

    private void thenAssertExportToBe(String expectedNafReport) {
        try {
            String nafReportAsString = nafXmlCreator.writeAsXml(this.nafReport);

            assertEquals(expectedNafReport.trim(), nafReportAsString.trim());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void givenContests() {
        Contest contest = new Contest();
        contest.setMatchDate(parseDateOrNullIgnoringExceptions("2024-02-26 14:00"));
        contest.setOpponents(List.of(
                newOpponent("HumanCoach", Race.human, 2, 1),
                newOpponent("OrcCoach", Race.orc, 1, 3)
        ));
        this.contests.add(contest);
    }

    private Team newOpponent(String coachName, Race race, Integer touchdowns, Integer casualties) {
        Team team = new Team();
        team.setCoachName(coachName);
        team.setRace(race);
        team.setScore(touchdowns);
        team.setInflictedcasualties(casualties);
        return team;
    }

    private Date parseDateOrNullIgnoringExceptions(String dateValue) {
        try {
            return dateFormat.parse(dateValue);
        } catch (ParseException e) {
            return null;
        }
    }
}
