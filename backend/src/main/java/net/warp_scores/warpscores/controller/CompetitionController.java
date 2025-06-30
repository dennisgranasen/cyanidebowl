package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.CompetitionStatsDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionStatsRepository;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository.CompetitionStatusCountRecord;
import net.warp_scores.warpscores.export.naf.NafReport;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStats;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.TeamAndRaceStats;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.MatchService;
import net.warp_scores.warpscores.service.NafExporter;
import net.warp_scores.warpscores.service.NafXmlCreator;
import net.warp_scores.warpscores.service.StatsService;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
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
    private final MatchService matchService;
    private final StatsService statsService;
    private final CompetitionStatsDomainService competitionStatsDomainService;
    private final CompetitionStatsRepository competitionStatsRepository;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeague(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Identity leagueIdentity = 
                new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus));

            log.info("Fetching competitions for league ID: {} with opus: {}", leagueId, opus);
            List<Competition> competitions =                
                competitionService.loadForLeague(leagueIdentity);
            log.info("Found {} competitions for league ID: {}", competitions.size(), leagueId);
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
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/league/{leagueId}/active")
    public ResponseEntity<List<Competition>> getActiveCompetitionsForLeague(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Identity leagueIdentity = 
                new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus));

            log.info("Fetching active competitions for league ID: {} with opus: {}", leagueId, opus);
            List<Competition> competitions =                
                competitionService.loadForLeague(leagueIdentity);
            log.info("Found {} competitions for league ID: {}", competitions.size(), leagueId);
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
            log.info("Returning {} active competitions for league ID: {}", filtered.size(), leagueId);
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
            Identity leagueIdentity = new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus));
            List<Competition> competitions =
                competitionService.loadForLeagueAndInitialize(leagueIdentity)
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

    @GetMapping(value = "/competitions/{competitionId}/stats", produces = "application/json")
    public ResponseEntity<CompetitionStats> getCompetitionStats(@PathVariable(name = "competitionId") UUID competitionId) {
        try
        {
            Optional<CompetitionStats> competitionStats = competitionStatsRepository.findById(competitionId);
            return competitionStats.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get stats for competition {}", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<Competition> getCompetition(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
                // Need to do this like leagueController... 

        String[] parts = competitionId.split(Identity.DELIMITER);
        Identity competitionIdentity = 
            new CompositeIdentity(ofNullable(opus).orElse(defaultOpus), parts);

        log.info("Fetching competition with ID: {} and opus: {}", competitionId, opus);
        Optional<Competition> competition =
            competitionService.loadCompetition(competitionIdentity);

        log.info(competitionId, opus, competition);
        return competition
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/competition/{competitionId}/exportNafData")
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
            Identity competitionIdentity = 
                new SimpleIdentity(competitionId, opus.orElse(defaultOpus));
            Optional<NafReport> nafReport = nafExporter.export(competitionIdentity, exporterName);
            Optional<String> xml = nafReport.map(nafXmlCreator::writeAsXml);
            byte[] result = xml.map(String::getBytes).orElse(null);
            output.setResult(result);
        } catch (Exception ex) {
            log.error("Unable to export naf data for {}.", competitionId, ex);
            output.setErrorResult("Error while exporting naf data.");
        }
    }

    @GetMapping("/competition/{competitionId}/teams")
    public ResponseEntity<List<Team>> getTeamsForCompetition(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Identity competitionIdentity = 
                new SimpleIdentity(competitionId, ofNullable(opus).orElse(defaultOpus));
            List<Team> teams =
                teamDomainService.findByCompetitionId(competitionIdentity);
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition id {}.", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}/team/{teamId}")
    public ResponseEntity<Team> getTeam(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "teamId") String teamId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Identity teamIdentity = 
                new SimpleIdentity(teamId, ofNullable(opus).orElse(defaultOpus));

            Identity competitionIdentity = 
                new SimpleIdentity(competitionId, ofNullable(opus).orElse(defaultOpus));

            Optional<Team> team = 
                teamDomainService.findTeam(teamIdentity/*, Optional.of(competitionIdentity)*/);
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for competition {} (teamId: {})", competitionId, teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/league/competitionCountByStatus")
    public ResponseEntity<List<CompetitionStatusCountRecord>> getCompetitionCountByStatus(
        @RequestParam(name = "leagueIds", required = true) String leagueIds) {
        log.info("Fetching competition count by status for leagues: {}", leagueIds);
        String[] parts = leagueIds.split(",");
        if (parts == null || parts.length == 0) {
            log.warn("No leagues provided for competition count");
            return ResponseEntity.badRequest().build();
        }
        try {
            List<Identity> ids = Stream.of(parts)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(IdentityUtil::fromId).toList();
            ids.forEach(id -> log.info("League ID: {}", id));
            return
                ResponseEntity.ok(competitionService.getCompetitionCountByStatus(ids));
        } catch (Exception e) {
            log.error("Error fetching competition count by status", e);
            return ResponseEntity.status(500).build();
        }   
    }
}
