package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LookupResponse extends ApiResponse {
    private IdWithName[] teams;
    private IdWithName[] leagues;
    private IdWithName[] coaches;
    private IdWithName[] competitions;
}
