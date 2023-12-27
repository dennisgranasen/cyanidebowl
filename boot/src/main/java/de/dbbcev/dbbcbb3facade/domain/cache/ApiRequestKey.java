package de.dbbcev.dbbcbb3facade.domain.cache;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;
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
