package net.warp_scores.warpscores.mongo;

import net.warp_scores.warpscores.ai.provider.copilot.GitHubCopilotLlmProvider;
import net.warp_scores.warpscores.config.MongoConfig;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class CopilotRatingQuotaUsageMongoConversionTest {
    @Test
    void storesNestedQuotaResetAsBsonDateAndReadsItBackAsUtcOffsetDateTime() throws Exception {
        var conversions = new MongoConfig().customConversions();
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();

        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();

        Instant resetInstant = Instant.parse("2026-11-01T00:00:00Z");
        CopilotRatingQuotaUsage usage = new CopilotRatingQuotaUsage();
        usage.setId("job-1");
        usage.setQuotaBefore(new GitHubCopilotLlmProvider.CopilotAccountQuota(
                false, 7000L, 602L, 91.4, 0.0,
                OffsetDateTime.parse("2026-11-01T01:00:00+01:00")));

        Document document = new Document();
        converter.write(usage, document);

        Document quota = document.get("quotaBefore", Document.class);
        assertThat(quota.get("resetDate")).isEqualTo(Date.from(resetInstant));

        CopilotRatingQuotaUsage restored = converter.read(CopilotRatingQuotaUsage.class, document);
        assertThat(restored.getQuotaBefore().resetDate().toInstant()).isEqualTo(resetInstant);
        assertThat(restored.getQuotaBefore().resetDate().getOffset()).isEqualTo(java.time.ZoneOffset.UTC);
    }
}