package net.warp_scores.warpscores.domain.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
public class ImageCache {
    @Id
    private String imageUrl;

    private Date lastAccess;
    private byte[] imageData;
}
