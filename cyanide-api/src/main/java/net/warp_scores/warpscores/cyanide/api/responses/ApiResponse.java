package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.ResponseMeta;

@Slf4j
public abstract class ApiResponse implements EmptyAwareResponse, UpdateChangeable {
    @Getter
    @Setter
    private Long[] size;
    @Getter
    @Setter
    private ResponseMeta meta;
    @Getter
    @Setter
    private Boolean promotional_content;
    @Getter
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

    public abstract void updateChangeableAttribute();

    final void updateChangeableAttributeTo(boolean changeable) {
        if (!changeable) {
            log.debug("Updating {} changeable attribute to {}.", this.getClass().getSimpleName(), changeable);
        }
        this.changeableResponse = changeable;
    }

    @Override
    public String toString() {
        return getInformationString();
    }

    protected abstract String getInformationString();
}
