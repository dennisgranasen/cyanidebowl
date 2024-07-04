package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.CompetitionTeamsRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionTeams;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionTeamsDomainService {
    private final CompetitionTeamsRepository competitionTeamsRepository;

    private final UUIDConverter uuidConverter;

    @Transactional
    public CompetitionTeams createOrUpdateCompetitionTeams(Competition competition, List<Team> teams) {

        CompetitionTeams competitionTeams = new CompetitionTeams();
        competitionTeams.setCompetitionUuid(competition.getUuid());
        competitionTeams.setTeamUuids(teams.stream().map(Team::getId).toList());
        return competitionTeamsRepository.save(competitionTeams);
    }

    public Optional<CompetitionTeams> findByCompetitionId(UUID competitionId) {
        return competitionTeamsRepository.findById(competitionId);
    }
}
