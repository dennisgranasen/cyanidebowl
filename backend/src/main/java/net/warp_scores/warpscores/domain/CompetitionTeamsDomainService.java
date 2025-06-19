package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.CompetitionTeamsRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionTeams;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionTeamsDomainService {
    private final CompetitionTeamsRepository competitionTeamsRepository;

    @Transactional
    public CompetitionTeams createOrUpdateCompetitionTeams(Competition competition,
                                                           List<Team> teams) {
        CompetitionTeams competitionTeams = new CompetitionTeams(competition.getIdentity());
        competitionTeams.setTeamIds(teams.stream().map(t -> t.getIdentity().getValue()).toList());
        return competitionTeamsRepository.save(competitionTeams);
    }

    public Optional<CompetitionTeams> findByCompetitionId(Identity competitionId) {
        return competitionTeamsRepository.findById(competitionId);
    }
}
