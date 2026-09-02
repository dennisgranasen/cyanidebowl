package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class LeagueStructureController {
    private final SeasonRepository seasons;
    private final PhaseRepository phases;
    private final StageRepository stages;
    private final StageSourceRepository selections;
    private final RegisteredSourceRepository registeredSources;
    private final DataCollectionRepository collections;

    @GetMapping("/admin/seasons/{seasonId}/phases")
    public ResponseEntity<List<Phase>> phases(@PathVariable String seasonId) {
        return seasons.existsById(seasonId)
                ? ResponseEntity.ok(phases.findBySeasonIdOrderBySequenceAsc(seasonId))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/admin/seasons/{seasonId}/phases")
    public ResponseEntity<Phase> createPhase(@PathVariable String seasonId, @RequestBody Phase phase) {
        return seasons.findById(seasonId).map(season -> {
            if (phase.getId() == null || phase.getId().isBlank()) phase.setId(UUID.randomUUID().toString());
            phase.setSeasonId(seasonId);
            phase.setLeagueSystemId(season.getLeagueSystemId());
            return ResponseEntity.status(201).body(phases.save(phase));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/phases/{phaseId}")
    public ResponseEntity<Phase> updatePhase(@PathVariable String phaseId, @RequestBody Phase phase) {
        return phases.findById(phaseId).map(existing -> {
            phase.setId(phaseId); phase.setSeasonId(existing.getSeasonId());
            phase.setLeagueSystemId(existing.getLeagueSystemId());
            return ResponseEntity.ok(phases.save(phase));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/phases/{phaseId}")
    public ResponseEntity<Void> deletePhase(@PathVariable String phaseId) {
        if (!phases.existsById(phaseId)) return ResponseEntity.notFound().build();
        List<Stage> children = stages.findByPhaseIdOrderByStepAscDisplayOrderAsc(phaseId);
        children.forEach(stage -> selections.deleteAll(selections.findByStageId(stage.getId())));
        stages.deleteAll(children); phases.deleteById(phaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/phases/{phaseId}/stages")
    public ResponseEntity<List<Stage>> stages(@PathVariable String phaseId) {
        return phases.existsById(phaseId)
                ? ResponseEntity.ok(stages.findByPhaseIdOrderByStepAscDisplayOrderAsc(phaseId))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/admin/phases/{phaseId}/stages")
    public ResponseEntity<Stage> createStage(@PathVariable String phaseId, @RequestBody Stage stage) {
        return phases.findById(phaseId).map(phase -> {
            if (stage.getId() == null || stage.getId().isBlank()) stage.setId(UUID.randomUUID().toString());
            stage.setPhaseId(phaseId); stage.setSeasonId(phase.getSeasonId());
            stage.setLeagueSystemId(phase.getLeagueSystemId());
            stage.setSequence(stage.getDisplayOrder());
            return ResponseEntity.status(201).body(stages.save(stage));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/seasons/{seasonId}/registered-sources")
    public ResponseEntity<List<RegisteredSource>> registeredSources(@PathVariable String seasonId) {
        return seasons.existsById(seasonId) ? ResponseEntity.ok(registeredSources.findBySeasonId(seasonId))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/admin/seasons/{seasonId}/registered-sources")
    public ResponseEntity<RegisteredSource> register(@PathVariable String seasonId,
            @RequestBody RegisteredSourceRequest request) {
        return seasons.findById(seasonId).map(season -> {
            RegisteredSource source = new RegisteredSource();
            source.setId(request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id());
            source.setSeasonId(seasonId); source.setLeagueSystemId(season.getLeagueSystemId());
            source.setSourceEntityId(IdentityUtil.fromId(request.sourceEntityId()));
            source.setSourceType(request.sourceType()); source.setGame(request.game());
            source.setPlatform(request.platform()); source.setRuleset(request.ruleset());
            source.setCollectionEnabled(!Boolean.FALSE.equals(request.collectionEnabled()));
            RegisteredSource saved = registeredSources.save(source);
            if (Boolean.TRUE.equals(saved.getCollectionEnabled()) && !collections.existsById(saved.getSourceEntityId()))
                collections.save(new DataCollection(saved.getSourceEntityId(), saved.getSourceType()));
            return ResponseEntity.status(201).body(saved);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/stages/{stageId}/match-selections")
    public ResponseEntity<StageSource> createSelection(@PathVariable String stageId,
            @RequestBody MatchSelectionRequest request) {
        return stages.findById(stageId).flatMap(stage -> registeredSources.findById(request.registeredSourceId())
                .filter(source -> source.getSeasonId().equals(stage.getSeasonId()))
                .map(source -> {
                    StageSource selection = new StageSource();
                    selection.setId(request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id());
                    selection.setStageId(stageId); selection.setSeasonId(stage.getSeasonId());
                    selection.setLeagueSystemId(stage.getLeagueSystemId()); selection.setRegisteredSourceId(source.getId());
                    selection.setSourceEntityId(source.getSourceEntityId()); selection.setSourceType(source.getSourceType());
                    selection.setGame(source.getGame()); selection.setPlatform(source.getPlatform()); selection.setRuleset(source.getRuleset());
                    selection.setFirstIndex(request.firstIndex()); selection.setLastIndex(request.lastIndex());
                    selection.setFirstId(request.firstId()); selection.setLastId(request.lastId());
                    selection.setIncludedMatchIds(request.includedMatchIds() == null ? List.of() : request.includedMatchIds());
                    selection.setExcludedMatchIds(request.excludedMatchIds() == null ? List.of() : request.excludedMatchIds());
                    selection.setIsArchived(request.isArchived());
                    return ResponseEntity.status(201).body(selections.save(selection));
                })).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/match-selections/{selectionId}")
    public ResponseEntity<StageSource> updateSelection(@PathVariable String selectionId,
            @RequestBody MatchSelectionRequest request) {
        return selections.findById(selectionId).map(selection -> {
            selection.setFirstIndex(request.firstIndex()); selection.setLastIndex(request.lastIndex());
            selection.setFirstId(request.firstId()); selection.setLastId(request.lastId());
            selection.setIncludedMatchIds(request.includedMatchIds() == null ? List.of() : request.includedMatchIds());
            selection.setExcludedMatchIds(request.excludedMatchIds() == null ? List.of() : request.excludedMatchIds());
            selection.setIsArchived(request.isArchived());
            return ResponseEntity.ok(selections.save(selection));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
