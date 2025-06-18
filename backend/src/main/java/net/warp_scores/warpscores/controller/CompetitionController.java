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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getActiveCompetitionsForLeague(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Competition> competitions =                
                competitionService.loadForLeague(leagueId, Optional.ofNullable(opus));

            // Check for null competitions and log the full list as JSON if any are found
            if (competitions.stream().anyMatch(c -> c == null)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(competitions);
                    log.error("Null competition found in list for league {}: {}", leagueId, json);
                } catch (Exception e) {
                    log.error("Failed to serialize competitions list to JSON", e);
                }
            }
            List<Competition> filtered = competitions.stream()
                    .filter(c -> c.getStatus() != null)
                    .sorted()
                    .toList();
            return ResponseEntity.ok(filtered);
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
            List<Competition> competitions =
                competitionService.loadForLeagueAndInitialize(leagueId, Optional.ofNullable(opus))
                    .stream()
                    .filter(c -> c.getStatus() != null)
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
        Optional<Competition> competition =
            competitionService.loadCompetition(competitionId, 
                                               Optional.ofNullable(opus));

        log.info(competitionId, opus, competition);
        return competition
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/competitions/{competitionId}/exportNafData")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public DeferredResult<byte[]> exportNafData(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus, 
            JwtAuthenticationToken principal) {
        String exporterName = Optional.ofNullable(principal).map(JwtAuthenticationToken::getName).orElse("unknown");
        DeferredResult<byte[]> output = new DeferredResult<>();
        nafExporterExecutor.execute(() -> 
            createNafExportData(competitionId, Optional.ofNullable(opus), exporterName, output));
        return output;
    }

    private void createNafExportData(String competitionId, Optional<Integer> opus, String exporterName, DeferredResult<byte[]> output) {
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
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Team> teams =
                teamDomainService.findByCompetitionId(competitionId, Optional.ofNullable(opus));
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition id {}.", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionId}/team/{teamId}")
    public ResponseEntity<Team> getTeam(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "teamId") String teamId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Optional<Team> team = 
                teamDomainService.findTeam(teamId,
                    Optional.of(competitionId), 
                    Optional.ofNullable(opus));
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for competition {} (teamId: {})", competitionId, teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
