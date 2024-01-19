package net.warp_scores.warpscores.domain.cache;

import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiRequestKey {
    private String requestClassName;
    private String apiRequestMd5Sum;

    public static ApiRequestKey newFor(ApiRequest requestObject) {
        return new ApiRequestKey(requestObject.getClass().getCanonicalName(), requestObject.md5Sum());
    }

    public String asString() {
        return String.format("%s[%s]", requestClassName, apiRequestMd5Sum);
    }
}
