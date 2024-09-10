package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.NafCoachRepository;
import net.warp_scores.warpscores.model.NafCoach;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NafCoachDomainService {

    public static final NafCoach NONE_NAF_COACH = new NafCoach(9, "Non-NAF",
            null);

    private final NafCoachRepository nafCoachRepository;

    @Transactional
    public Optional<NafCoach> findByName(String coachName) {
        return nafCoachRepository.findByNafNameIgnoreCase(coachName);
    }

    @Transactional
    public NafCoach store(NafCoach coach) {
        return nafCoachRepository.save(coach);
    }
}
