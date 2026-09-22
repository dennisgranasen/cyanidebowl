package net.warp_scores.warpscores.mongo;

import net.warp_scores.warpscores.identity.Identity;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IdentityWriteConverter implements Converter<Identity, Document> {
    @Override
    public Document convert(Identity source) {
        Document doc = new Document();
        doc.put("type", source.getClass().getSimpleName());
        doc.put("value", source.asMongoKey());
        return doc;
    }
}
