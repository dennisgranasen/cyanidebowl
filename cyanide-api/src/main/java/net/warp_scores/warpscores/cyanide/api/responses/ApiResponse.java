package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.ResponseMeta;

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

    @Override
    public String toString() {
        return getInformationString();
    }

    protected abstract String getInformationString();
}
