package net.warp_scores.warpscores.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
public class IdWithName {
    @JsonAlias({"_id"})
    private String id;
    private String name;
}
