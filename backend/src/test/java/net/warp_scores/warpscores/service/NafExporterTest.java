package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private NafCoachLookupClient nafCoachLookupClient;

    private NafXmlCreator nafXmlCreator = new NafXmlCreator();

    @InjectMocks
    private NafExporter nafExporter;

    @BeforeEach
    public void setup() throws IOException {
        Mockito.when(nafCoachLookupClient.lookupNafCoach(eq("HumanCoach")))
                .thenReturn(new NafCoachLookupClient.NafCoach(
                        "HumanCoach", 42, null));
        Mockito.when(nafCoachLookupClient.lookupNafCoach(eq("OrcCoach")))
                .thenReturn(new NafCoachLookupClient.NafCoach(
                        "OrcCoach", 23, null));
    }

    @Test
    public void testExportContests() throws IOException {
        givenContests();

        whenExported();

        thenAssertExportToBe("/expectedNafExport.xml");
    }

    private void whenExported() throws IOException {
        nafReport = nafExporter.export(contests, "Test");
    }

    private void thenAssertExportToBe(String nafReportFile) {
        try {
            String expectedNafReport = Files.readString(Paths.get(this.getClass().getResource(nafReportFile).toURI()),
                    Charset.defaultCharset());

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
