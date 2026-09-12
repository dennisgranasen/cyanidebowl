package net.warp_scores.warpscores.ai.provider;

public record OutputContract(Format format, String schemaJson) {
    public enum Format { TEXT, JSON }

    public OutputContract {
        if (format == null) format = Format.TEXT;
        if (format == Format.JSON && (schemaJson == null || schemaJson.isBlank())) {
            throw new IllegalArgumentException("JSON output requires schemaJson");
        }
    }

    public static OutputContract text() {
        return new OutputContract(Format.TEXT, null);
    }
}
