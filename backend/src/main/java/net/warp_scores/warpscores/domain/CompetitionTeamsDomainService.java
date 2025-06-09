package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.CompetitionTeamsRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionTeams;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.IdService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionTeamsDomainService {
    private final CompetitionTeamsRepository competitionTeamsRepository;
    private final IdService idService;
    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public CompetitionTeams createOrUpdateCompetitionTeams(Competition competition,
                                                           List<Team> teams) {

        CompetitionTeams competitionTeams = new CompetitionTeams();
        competitionTeams.setCompetitionId(competition.getId());
        competitionTeams.setTeamIds(teams.stream().map(Team::getId).toList());
        return competitionTeamsRepository.save(competitionTeams);
    }

    public Optional<CompetitionTeams> findByCompetitionId(
            String competitionId, Optional<Integer> opus) {
        String id = idService.getComposedId(opus, competitionId);
        return competitionTeamsRepository.findById(id);
    }
   
}
