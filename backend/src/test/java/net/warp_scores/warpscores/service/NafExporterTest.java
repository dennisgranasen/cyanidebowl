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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static net.warp_scores.warpscores.domain.NafCoachDomainService.NONE_NAF_COACH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class NafExporterTest {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMANY);

    private final List<Contest> contests = new ArrayList<>();

    private NafReport nafReport;

    @Mock
    private NafCoachService nafCoachService;

    private final NafXmlCreator nafXmlCreator = new NafXmlCreator();

    @InjectMocks
    private NafExporter nafExporter;

    public NafExporterTest() {
    }

    @BeforeEach
    public void setup() {
        Mockito.when(nafCoachService.lookupCoach(eq("HumanCoach")))
                .thenReturn(Optional.of(new NafCoach(
                        42, "HumanCoach", null)));
        Mockito.when(nafCoachService.lookupCoach(eq("OrcCoach")))
                .thenReturn(Optional.of(new NafCoach(
                        23, "OrcCoach", null)));
        Mockito.when(nafCoachService.lookupCoach(eq("UnknownCoach")))
                .thenReturn(Optional.of(NONE_NAF_COACH));
    }

    @Test
    public void testExportContests() {
        givenContests();

        whenExported();

        String expectedXml = String.format(
                "<?xml version='1.0' encoding='UTF-8'?><nafReport><organiser>warp-scores.net//Test</organiser><coaches>%s</coaches>%s</nafReport>",
                getCoachesXml(List.of(
                                new String[]{"HumanCoach", "42", "Human"},
                                new String[]{"Non-NAF", "9", "Multiple Races"},
                                new String[]{"OrcCoach", "23", "Orc"}
                        )
                ),
                getGamesXml(List.of(
                        new String[]{
                                "2024-02-26 14:00",
                                "HumanCoach",
                                "42",
                                "Human",
                                "2",
                                "1",
                                "OrcCoach",
                                "23",
                                "Orc",
                                "1",
                                "3",
                        },
                        new String[]{
                                "2024-02-26 15:00",
                                "HumanCoach",
                                "42",
                                "Human",
                                "4",
                                "2",
                                "Non-NAF",
                                "9",
                                "Amazon",
                                "2",
                                "4",
                        },
                        new String[]{
                                "2024-02-26 16:00",
                                "Non-NAF",
                                "9",
                                "Human",
                                "1",
                                "1",
                                "OrcCoach",
                                "23",
                                "Orc",
                                "1",
                                "1",
                        }
                )));

        thenAssertExportToBe(expectedXml);
    }

    private String getGamesXml(List<String[]> gamesData) {
        StringBuilder gamesXml = new StringBuilder();
        for (String[] gameData : gamesData) {
            String gameTemplate = "<game><timeStamp>%s</timeStamp>%s%s</game>";
            String playerRecordTemplate = "<playerRecord><name>%s</name><number>%s</number><team>%s</team><teamRating>100</teamRating><touchDowns>%s</touchDowns><badlyHurt>%s</badlyHurt></playerRecord>";
            gamesXml.append(
                    String.format(gameTemplate, gameData[0],
                            String.format(playerRecordTemplate, gameData[1], gameData[2], gameData[3], gameData[4],
                                    gameData[5]),
                            String.format(playerRecordTemplate, gameData[6], gameData[7], gameData[8], gameData[9],
                                    gameData[10])));
        }
        return gamesXml.toString();
    }

    private String getCoachesXml(List<String[]> coachesData) {
        StringBuilder coachesXml = new StringBuilder();

        for (String[] coachData : coachesData) {
            coachesXml.append(
                    String.format("<coach><name>%s</name><number>%s</number><team>%s</team></coach>", coachData[0],
                            coachData[1], coachData[2]));
        }
        return coachesXml.toString();
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
        this.contests.add(createContest("2024-02-26 14:00",
                asArray("HumanCoach", "OrcCoach"),
                asArray(Race.human, Race.orc),
                asArray(2, 1),
                asArray(1, 3)));
        this.contests.add(createContest("2024-02-26 15:00",
                asArray("HumanCoach", "UnknownCoach"),
                asArray(Race.human, Race.amazon),
                asArray(4, 2),
                asArray(2, 4)));
        this.contests.add(createContest("2024-02-26 16:00",
                asArray("UnknownCoach", "OrcCoach"),
                asArray(Race.human, Race.orc),
                asArray(1, 1),
                asArray(1, 1)));
        this.contests.add(createContest("2024-02-26 17:00",
                asArray(NafCoachService.BB3_AI_COACH_NAME, "OrcCoach"),
                asArray(Race.human, Race.orc),
                asArray(1, 1),
                asArray(1, 1)));
    }

    private static <Type> Object[] asArray(Type first, Type second) {
        List<Type> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        return list.toArray(new Object[0]);
    }

    private Contest createContest(String matchDateValue, Object[] teamCoachNames,
            Object[] teamRaces,
            Object[] teamTouchdowns,
            Object[] teamCasualties) {
        Contest contest = new Contest();
        contest.setMatchDate(parseDateOrNullIgnoringExceptions(matchDateValue));
        contest.setOpponents(List.of(
                newOpponent((String) teamCoachNames[0], (Race) teamRaces[0], (Integer) teamTouchdowns[0],
                        (Integer) teamCasualties[0]),
                newOpponent((String) teamCoachNames[1], (Race) teamRaces[1], (Integer) teamTouchdowns[1],
                        (Integer) teamCasualties[1])
        ));
        return contest;
    }

    private Team newOpponent(String coachName, Race race, Integer touchdowns, Integer casualties) {
        Team team = new Team();
        team.setId(UUID.randomUUID());
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
