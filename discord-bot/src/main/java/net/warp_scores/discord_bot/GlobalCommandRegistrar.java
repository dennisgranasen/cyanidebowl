package net.warp_scores.discord_bot;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalCommandRegistrar implements ApplicationRunner {

    private final RestClient client;

    @Value("${guildId}")
    private String guildId;

    //This method will run only once on each start up and is automatically called with Spring so blocking is okay.
    @Override
    public void run(ApplicationArguments args) throws IOException {
        //Create an ObjectMapper that supported Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below.
        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final ApplicationService applicationService = client.getApplicationService();
        final long applicationId = client.getApplicationId().block();

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (Resource resource : matcher.getResources("commands/*.json")) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                    .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

            commands.add(request);
        }

        List<ApplicationCommandRequest> globalCommands = commands.stream().filter(cmd -> "help" .equals(cmd.name()))
                .toList();
        List<ApplicationCommandRequest> guildCommands = commands.stream().filter(cmd -> !"help" .equals(cmd.name()))
                .toList();
        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId,
                        globalCommands)
                .doOnNext(ignore -> log.debug("Successfully registered global commands"))
                .doOnError(e -> log.error("Failed to register global commands", e))
                .subscribe();

        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, Long.valueOf(guildId.trim()),
                        guildCommands)
                .doOnNext(ignore -> log.debug("Successfully registered guild commands"))
                .doOnError(e -> log.error("Failed to register guild  commands", e))
                .subscribe();
    }
}
