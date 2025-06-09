package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.NafExporter;
import net.warp_scores.warpscores.service.NafXmlCreator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    private final TeamDomainService teamDomainService;
    private final NafExporter nafExporter;
    private final NafXmlCreator nafXmlCreator;

    private final ExecutorService nafExporterExecutor = Executors.newFixedThreadPool(5);

    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getActiveCompetitionsForLeague(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Competition> competitions;
            if (opus != null && opus < 3) {
                // If opus is less than 3, we assume it's an old ID
                Integer oldId = Integer.parseInt(leagueId);
                competitions = 
                    competitionService.loadForLeague(oldId, Optional.ofNullable(opus));

            } 
            else {
                UUID leagueUuid = UUID.fromString(leagueId);
                competitions = 
                    competitionService.loadForLeague(leagueUuid, Optional.ofNullable(opus));
            }
            competitions = competitions
                    .stream()
                    .sorted()
                    .toList();
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/league/{leagueId}/initialized")
    public ResponseEntity<List<Competition>> getActiveCompetitionsForLeagueInitialized(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Competition> competitions;
            if (opus != null && opus < 3) {
                // If opus is less than 3, we assume it's an old ID
                Integer oldId = Integer.parseInt(leagueId);
                competitions = 
                    competitionService.loadForLeagueAndInitialize(oldId, Optional.ofNullable(opus));

            } 
            else {
                UUID leagueUuid = UUID.fromString(leagueId);
                competitions = 
                    competitionService.loadForLeagueAndInitialize(leagueUuid, Optional.ofNullable(opus));
            }
            competitions = competitions
                    .stream()
                    .sorted()
                    .toList();
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionId}")
    public ResponseEntity<Competition> getCompetition(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
                // Need to do this like leagueController... 

        log.info("Fetching competition with ID: {} and opus: {}", competitionId, opus);
        Optional<Competition> competition;
        if (opus != null && opus < 3) {
            competition = competitionService.loadCompetitionByOldId(
                Integer.parseInt(competitionId), Optional.ofNullable(opus));
                log.info("Did < 3 opus, using old ID: {}", competitionId);
        }
        else {
            competition = competitionService.loadCompetition(
                UUID.fromString(competitionId), Optional.ofNullable(opus));
                log.info("Did > 2 opus, using UUID: {}", competitionId);
        }
        log.info(competitionId, opus, competition);
        return competition
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/competitions/{competitionId}/exportNafData")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public DeferredResult<byte[]> exportNafData(
            @PathVariable(name = "competitionId") UUID competitionId,
            @RequestParam(name = "opus", required = false) Integer opus, 
            JwtAuthenticationToken principal) {
        String exporterName = Optional.ofNullable(principal).map(JwtAuthenticationToken::getName).orElse("unknown");
        DeferredResult<byte[]> output = new DeferredResult<>();
        nafExporterExecutor.execute(() -> 
            createNafExportData(competitionId, Optional.ofNullable(opus), exporterName, output));
        return output;
    }

    private void createNafExportData(UUID competitionId, Optional<Integer> opus, String exporterName, DeferredResult<byte[]> output) {
        try {
            Optional<NafReport> nafReport = nafExporter.export(competitionId, opus, exporterName);
            Optional<String> xml = nafReport.map(nafXmlCreator::writeAsXml);
            byte[] result = xml.map(String::getBytes).orElse(null);
            output.setResult(result);
        } catch (Exception ex) {
            log.error("Unable to export naf data for {}.", competitionId, ex);
            output.setErrorResult("Error while exporting naf data.");
        }
    }

    @GetMapping("/competitions/{competitionId}/teams")
    public ResponseEntity<List<Team>> getTeamsForCompetition(
            @PathVariable(name = "competitionId") UUID competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Team> teams =
                teamDomainService.findByCompetitionId(competitionId, Optional.ofNullable(opus));
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition uuid {}.", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionUuid}/team/{teamUuid}")
    public ResponseEntity<Team> getTeam(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "teamUuid") UUID teamUuid) {
        try {
            Optional<Team> team = teamDomainService.findTeam(teamUuid, Optional.of(competitionUuid));
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for competition {} (teamId: {})", competitionUuid, teamUuid, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
