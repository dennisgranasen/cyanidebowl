package net.warp_scores.warpscores.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class IdService {
    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    public String getComposedId(Optional<Integer> opus, String id) {
        return opus.orElse(defaultOpus) + "_" + id;
    }
}