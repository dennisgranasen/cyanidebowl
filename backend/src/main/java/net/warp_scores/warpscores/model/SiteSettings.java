package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "siteSettings")
@Getter
@Setter
public class SiteSettings {
    public static final String SITE_ID = "site";

    @Id
    private String id = SITE_ID;

    /**
     * UI locale used when a signed-in user has no explicit preference, and
     * for anonymous visitors without a local override.
     */
    private String defaultLocale = "sv";
}
