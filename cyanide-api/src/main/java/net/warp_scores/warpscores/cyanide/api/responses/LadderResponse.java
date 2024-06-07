package net.warp_scores.warpscores.cyanide.api.responses;

public class LadderResponse extends ApiResponse {
    @Override
    public String getInformationString() {
        return String.format("LadderResponse[isEmpty=%s, changeable=%s]",
                isEmpty(),
                isChangeableResponse());
    }
}
