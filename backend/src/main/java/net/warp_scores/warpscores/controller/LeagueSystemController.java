package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class LeagueSystemController {

    private final LeagueSystemRepository leagueSystems;
    private final SeasonRepository seasons;
    private final StageRepository stages;
    private final StageSourceRepository stageSources;

    @GetMapping("/admin/league-systems")
    public List<LeagueSystem> getLeagueSystems() {
        return leagueSystems.findAll();
    }

    @PostMapping("/admin/league-systems")
    public ResponseEntity<LeagueSystem> createLeagueSystem(@RequestBody LeagueSystem leagueSystem) {
        leagueSystem.setId(UUID.randomUUID().toString());
        return ResponseEntity.status(201).body(leagueSystems.save(leagueSystem));
    }

    @GetMapping("/admin/league-systems/{leagueSystemId}")
    public ResponseEntity<LeagueSystem> getLeagueSystem(@PathVariable String leagueSystemId) {
        return leagueSystems.findById(leagueSystemId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/league-systems/{leagueSystemId}")
    public ResponseEntity<LeagueSystem> updateLeagueSystem(
            @PathVariable String leagueSystemId,
            @RequestBody LeagueSystem leagueSystem) {
        if (!leagueSystems.existsById(leagueSystemId)) {
            return ResponseEntity.notFound().build();
        }
        leagueSystem.setId(leagueSystemId);
        return ResponseEntity.ok(leagueSystems.save(leagueSystem));
    }

    @DeleteMapping("/admin/league-systems/{leagueSystemId}")
    public ResponseEntity<Void> deleteLeagueSystem(@PathVariable String leagueSystemId) {
        for (Season season : seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId)) {
            deleteSeasonDescendants(season.getId());
        }
        leagueSystems.deleteById(leagueSystemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/league-systems/{leagueSystemId}/seasons")
    public ResponseEntity<List<Season>> getSeasons(@PathVariable String leagueSystemId) {
        if (!leagueSystems.existsById(leagueSystemId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId));
    }

    @PostMapping("/admin/league-systems/{leagueSystemId}/seasons")
    public ResponseEntity<Season> createSeason(@PathVariable String leagueSystemId, @RequestBody Season season) {
        if (!leagueSystems.existsById(leagueSystemId)) {
            return ResponseEntity.notFound().build();
        }
        if (season.getNumber() == null) {
            return ResponseEntity.badRequest().build();
        }
        season.setLeagueSystemId(leagueSystemId);
        normalizeSeason(season);
        return ResponseEntity.status(201).body(seasons.save(season));
    }

    @GetMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<Season> getSeason(@PathVariable String seasonId) {
        return seasons.findById(seasonId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<Season> updateSeason(@PathVariable String seasonId, @RequestBody Season season) {
        if (season.getNumber() == null) {
            return ResponseEntity.badRequest().build();
        }
        return seasons.findById(seasonId)
                .map(existing -> {
                    season.setId(seasonId);
                    season.setLeagueSystemId(existing.getLeagueSystemId());
                    normalizeSeason(season);
                    return ResponseEntity.ok(seasons.save(season));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<Void> deleteSeason(@PathVariable String seasonId) {
        if (!seasons.existsById(seasonId)) {
            return ResponseEntity.notFound().build();
        }
        deleteSeasonDescendants(seasonId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/seasons/{seasonId}/stages")
    public ResponseEntity<List<Stage>> getStages(@PathVariable String seasonId) {
        if (!seasons.existsById(seasonId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stages.findBySeasonIdOrderBySequenceAsc(seasonId));
    }

    @PostMapping("/admin/seasons/{seasonId}/stages")
    public ResponseEntity<Stage> createStage(@PathVariable String seasonId, @RequestBody Stage stage) {
        return seasons.findById(seasonId)
                .map(season -> {
                    stage.setSeasonId(seasonId);
                    stage.setLeagueSystemId(season.getLeagueSystemId());
                    return ResponseEntity.status(201).body(stages.save(stage));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/stages/{stageId}")
    public ResponseEntity<Stage> getStage(@PathVariable String stageId) {
        return stages.findById(stageId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/stages/{stageId}")
    public ResponseEntity<Stage> updateStage(@PathVariable String stageId, @RequestBody Stage stage) {
        return stages.findById(stageId)
                .map(existing -> {
                    stage.setId(stageId);
                    stage.setSeasonId(existing.getSeasonId());
                    stage.setLeagueSystemId(existing.getLeagueSystemId());
                    return ResponseEntity.ok(stages.save(stage));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/stages/{stageId}")
    public ResponseEntity<Void> deleteStage(@PathVariable String stageId) {
        if (!stages.existsById(stageId)) {
            return ResponseEntity.notFound().build();
        }
        deleteStageDescendants(stageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/stages/{stageId}/sources")
    public ResponseEntity<List<StageSource>> getStageSources(@PathVariable String stageId) {
        if (!stages.existsById(stageId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stageSources.findByStageId(stageId));
    }

    @PostMapping("/admin/stages/{stageId}/sources")
        public ResponseEntity<StageSource> createStageSource(
            @PathVariable String stageId,
            @RequestBody StageSourceRequest request) {
        return stages.findById(stageId)
                .map(stage -> {
                StageSource source = stageSource(request);
                    source.setStageId(stageId);
                    source.setSeasonId(stage.getSeasonId());
                    source.setLeagueSystemId(stage.getLeagueSystemId());
                    return ResponseEntity.status(201).body(stageSources.save(source));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/stage-sources/{sourceId}")
    public ResponseEntity<StageSource> getStageSource(@PathVariable String sourceId) {
        return stageSources.findById(sourceId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/stage-sources/{sourceId}")
        public ResponseEntity<StageSource> updateStageSource(
            @PathVariable String sourceId,
            @RequestBody StageSourceRequest request) {
        return stageSources.findById(sourceId)
                .map(existing -> {
                StageSource source = stageSource(request);
                    source.setId(sourceId);
                    source.setStageId(existing.getStageId());
                    source.setSeasonId(existing.getSeasonId());
                    source.setLeagueSystemId(existing.getLeagueSystemId());
                    return ResponseEntity.ok(stageSources.save(source));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/stage-sources/{sourceId}")
    public ResponseEntity<Void> deleteStageSource(@PathVariable String sourceId) {
        if (!stageSources.existsById(sourceId)) {
            return ResponseEntity.notFound().build();
        }
        stageSources.deleteById(sourceId);
        return ResponseEntity.noContent().build();
    }

    private void deleteSeasonDescendants(String seasonId) {
        List<Stage> seasonStages = stages.findBySeasonIdOrderBySequenceAsc(seasonId);
        for (Stage stage : seasonStages) {
            deleteStageDescendants(stage.getId());
        }
        stages.deleteAll(seasonStages);
        seasons.deleteById(seasonId);
    }

    private void deleteStageDescendants(String stageId) {
        stageSources.deleteAll(stageSources.findByStageId(stageId));
    }

        private void normalizeSeason(Season season) {
            season.setSequence(season.getNumber());
            if (season.getName() == null || season.getName().isBlank()) {
                season.setName("Season " + season.getNumber());
            }
        }

    private StageSource stageSource(StageSourceRequest request) {
        StageSource source = new StageSource();
        source.setId(request.id());
        source.setSourceEntityId(IdentityUtil.fromId(request.sourceEntityId()));
        source.setSourceType(request.sourceType());
        source.setGame(request.game());
        source.setPlatform(request.platform());
        source.setRuleset(request.ruleset());
        source.setFirstIndex(request.firstIndex());
        source.setLastIndex(request.lastIndex());
        source.setFirstId(request.firstId());
        source.setLastId(request.lastId());
        source.setIsArchived(request.isArchived());
        source.setLegacyCircuitId(request.legacyCircuitId());
        source.setLegacyCircuitLegId(request.legacyCircuitLegId());
        source.setLegacyEntityIndex(request.legacyEntityIndex());
        return source;
    }
}
