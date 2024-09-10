package net.warp_scores.discord_bot;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalCommandRegistrar implements ApplicationRunner {

    private final RestClient client;

    private final WarpScoresProperties warpScoresProperties;

    //This method will run only once on each start up and is automatically called with Spring so blocking is okay.
    @Override
    public void run(ApplicationArguments args) throws IOException {
        //Create an ObjectMapper that supported Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below.
        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final ApplicationService applicationService = client.getApplicationService();
        final Long applicationId = client.getApplicationId().block();
        if (applicationId == null) {
            log.error("Unable to get application id from discord.");
            return;
        }

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        Optional<Long> testGuildId = Optional.ofNullable(warpScoresProperties.getDiscord().getTestGuildId());
        for (Resource resource : matcher.getResources("commands/*.json")) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                    .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

            if (testGuildId.isEmpty()) {
                commands.add(request);
            } else {
                commands.add(ApplicationCommandRequest.builder()
                        .from(request).name("test" + request.name()).build());
            }
        }

        if (testGuildId.isEmpty()) {
            applicationService.bulkOverwriteGlobalApplicationCommand(applicationId,
                            commands)
                    .doOnNext(commandData -> log.info("Successfully registered global command ({}).", commandData.name()))
                    .doOnError(e -> log.error("Failed to register global commands.", e))
                    .subscribe();
        } else {
            applicationService.bulkOverwriteGuildApplicationCommand(applicationId, testGuildId.get(),
                            commands)
                    .doOnNext(commandData -> log.info("Successfully registered test guild command ({}).", commandData.name()))
                    .doOnError(e -> log.error("Failed to register test guild commands", e))
                    .subscribe();
        }
    }
}
