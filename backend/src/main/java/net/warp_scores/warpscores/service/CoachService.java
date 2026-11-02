package net.warp_scores.warpscores.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.CoachRepository;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Coach;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coaches;

    public List<Coach> all() {
        return coaches.findAll().stream()
                .sorted(Comparator.comparing(Coach::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public Coach updateMappings(String id, String bb2CoachId, String bb3CoachId) {
        Coach coach = coaches.findById(IdentityUtil.fromId(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        coach.setBb2CoachId(clean(bb2CoachId));
        coach.setBb3CoachId(clean(bb3CoachId));
        return coaches.save(coach);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
