package net.warp_scores.warpscores.mongo;

import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IdentityReadConverter implements Converter<Document, Identity> {
    @Override
    public Identity convert(Document source) {
        String type = source.getString("type");
        String value = source.getString("value");
        if ("SimpleIdentity".equals(type)) {
            return SimpleIdentity.fromId(value);
        } else if ("CompositeIdentity".equals(type)) {
            return CompositeIdentity.fromId(value);
        }
        throw new IllegalArgumentException("Unknown Identity type: " + type);
    }
}
