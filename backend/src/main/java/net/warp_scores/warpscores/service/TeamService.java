package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLeg;
import net.warp_scores.warpscores.model.CircuitLegEntity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final CircuitRepository circuitRepository;
    private final CyanideApiService cyanideApiService;

    @Value("${cyanide.defaults.pageLimit:100}")
    private int defaultPageLimit;

    @DurationLogging
    public Optional<Team> loadById(Identity teamId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isPresent()) {
            log.info("Loaded team by ID: {}", teamId);
            return team;
        } else {
            log.warn("Team with ID {} not found in repository. Loading from cyanide", teamId);
            team = cyanideApiService.loadTeam(teamId, true, ofNullable(true));
            if (team.isEmpty()) {
                log.warn("Team with ID {} not found in Cyanide API", teamId);
                return Optional.empty();
            }
            log.info("Team {} loaded from Cyanide API, saving to repository", teamId);
            teamRepository.save(team.get());
            return team;
        }
    }

    @DurationLogging
    public List<Team> getTeamsForLeague(Identity leagueId) {
        List<Team> teams = teamRepository.findByLeagueId(leagueId);
        if (teams.isEmpty()) {
            log.warn("No teams found for league ID {}", leagueId);
            return List.of();
        }
        log.info("Found {} teams for league ID {}", teams.size(), leagueId);
        return teams;
    } 

    @DurationLogging
    public List<Team> getTeamsForCompetition(Identity competitionId) { 
        List<Team> teams = teamRepository.findByCompetitionId(competitionId);
        if (teams.isEmpty()) {
            log.warn("No teams found for competition ID {}", competitionId);
            return List.of();
        }
        log.info("Found {} teams for competition ID {}", teams.size(), competitionId);
        return teams;
    }

    @DurationLogging
    public List<Team> getTeamsForCircuit(long circuitId) {
        // 1. Find all circuitlegs for the circuit
        Optional<Circuit> circuit = circuitRepository.findById(circuitId);
        if (circuit.isEmpty()) {
            log.warn("Circuit with ID {} not found.", circuitId);
            return List.of();
        }
        

        // circuit.get().getCircuitLegs().forEach(leg -> 
        // {
        //     log.info("Circuit leg: {}", leg);
        //     leg.getEntities().forEach(entity -> 
        //     {
        //         log.info("  Entity: {} {} {} {}", entity.getEntityId(), entity.getLegType(), entity.getPlatform(), entity.getGame());
        //     });
        // });

        // 2. Find all leagues for those circuit legs
        Set<Identity> leagueIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.League)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        Set<Team> teams = leagueIds.stream()
                .flatMap(leagueId -> teamRepository.findByLeagueId(leagueId).stream())
                .collect(Collectors.toSet());

        // 3. Find all competitions for those circuit legs
        Set<Identity> competitionIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.Competition)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        Set<Team> teamsFromCompetitions = competitionIds.stream()
                .flatMap(compId -> teamRepository.findByCompetitionId(compId).stream()) 
                .collect(Collectors.toSet());
        
        teams.addAll(teamsFromCompetitions);

        // 4. Recursively find all teams in nested circuits
        Set<Identity> circuitIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.Circuit)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        if (!circuitIds.isEmpty()) {
            log.info("Recursively loading teams for nested circuits: {}", circuitIds);
            Set<Team> teamsFromCircuits = circuitIds.stream()
                .flatMap(id -> getTeamsForCircuit(Long.parseLong(id.toString())).stream())
                .collect(Collectors.toSet());
            teams.addAll(teamsFromCircuits);
        }
        log.info("Found {} teams for circuit ID {}", teams.size(), circuitId);
        
        return teams.stream().toList();
    }


    @DurationLogging
    public List<Team> getTeamsForCircuitLeg(long circuitId, long circuitLegId) {
        // 1. Find all circuitlegs for the circuit
        Optional<Circuit> circuit = circuitRepository.findById(circuitId);
        if (circuit.isEmpty()) {
            log.warn("Circuit with ID {} not found.", circuitId);
            return List.of();
        }
        Optional<CircuitLeg> legOpt = circuit.get().getCircuitLegs().stream()
                .filter(cl -> cl.getCircuitLegId().equals(circuitLegId))
                .findFirst();
        if (legOpt.isEmpty()) {
            log.warn("Circuit leg with ID {} not found.", circuitLegId);
            return List.of();
        }
        CircuitLeg circuitLeg = legOpt.get(); 

        // circuit.get().getCircuitLegs().forEach(leg -> 
        // {
        //     log.info("Circuit leg: {}", leg);
        //     leg.getEntities().forEach(entity -> 
        //     {
        //         log.info("  Entity: {} {} {} {}", entity.getEntityId(), entity.getLegType(), entity.getPlatform(), entity.getGame());
        //     });
        // });

        // 2. Find all leagues for those circuit legs
        return circuitLeg.getEntities().stream()                
                .flatMap(entity -> getTeamsForCircuitLegEntity(circuitId, circuitLegId, entity.getEntityId()).stream())
                .collect(Collectors.toList());

                /* 
        Set<Identity> leagueIds = circuitLeg.getEntities().stream()
                .filter(entity -> entity.getLegType() == EntityType.League)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        Set<Team> teams = leagueIds.stream()
                .flatMap(leagueId -> teamRepository.findByLeagueId(leagueId).stream())
                .collect(Collectors.toSet());
        // 3. Find all competitions for those circuit legs
        Set<Identity> competitionIds = circuitLeg.getEntities().stream()
                .filter(entity -> entity.getLegType() == EntityType.Competition)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());   
        Set<Team> teamsFromCompetitions = competitionIds.stream()
                .flatMap(compId -> teamRepository.findByCompetitionId(compId).stream()) 
                .collect(Collectors.toSet());
        teams.addAll(teamsFromCompetitions);
        // 4. Recursively find all teams in nested circuits
        Set<Identity> circuitIds = circuitLeg.getEntities().stream()
                .filter(entity -> entity.getLegType() == EntityType.Circuit)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        if (!circuitIds.isEmpty()) {
            log.info("Recursively loading teams for nested circuits: {}", circuitIds);  
            Set<Team> teamsFromCircuits = circuitIds.stream()
                .flatMap(id -> getTeamsForCircuit(Long.parseLong(id.toString())).stream())
                .collect(Collectors.toSet());
            teams.addAll(teamsFromCircuits);
        }
        log.info("Found {} teams for circuit leg ID {}", teams.size(), circuitLegId);
        return teams.stream().toList();
         */
    }

    @DurationLogging
    public List<Team> getTeamsForCircuitLegEntity(long circuitId, long circuitLegId, Identity entityId) {
        // 1. Find all circuit legs for the circuit
        Optional<Circuit> circuit = circuitRepository.findById(circuitId);
        if (circuit.isEmpty()) {
            log.warn("Circuit with ID {} not found.", circuitId);
            return List.of();
        }
        Optional<CircuitLeg> legOpt = circuit.get().getCircuitLegs().stream()
                .filter(cl -> cl.getCircuitLegId().equals(circuitLegId))
                .findFirst();

         if (legOpt.isEmpty()) {
            log.warn("Circuit leg with ID {} not found.", circuitLegId);
            return List.of();
        }
        Optional<CircuitLegEntity> entityOpt = legOpt.get().getEntities().stream()
                .filter(e -> e.getEntityId().equals(entityId))
                .findFirst();
        if (entityOpt.isEmpty()) {
            log.warn("Circuit leg entity with ID {} not found.", entityId);

            return List.of();
        }   
        CircuitLegEntity entity = entityOpt.get();
        switch(entity.getLegType()) {
            case League:
                return getTeamsForLeague(entity.getEntityId());
            case Competition:
                return getTeamsForCompetition(entity.getEntityId());
            case Circuit:
                return getTeamsForCircuit(Long.parseLong(entity.getEntityId().getValue()));
            default:
                log.warn("Unsupported entity type {} for entity ID {}", entity.getLegType(), entity.getEntityId());
                return List.of();
        }   
    }



    @DurationLogging
    public List<Team> loadAll() {
        return teamRepository.findAll();
    }

}