package de.dbbcev.dbbcbb3facade.cyanide.api.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ResponseMeta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ApiResponse implements EmptyAwareResponse, UpdateChangeable {
    private Long[] size;
    private ResponseMeta meta;
    private Boolean promotional_content;
    private boolean changeableResponse = true;

    @Getter
    @Setter
    public static class ImageUrls {
        private String logos;
        private String races;
        private String portraits;
        private String skills;
        private String stadiums;
    }

    @Getter
    @Setter
    public static class Urls {
        private ImageUrls images;
    }

    public void updateChangeableAttribute() {
        this.changeableResponse = true;
    }
}
