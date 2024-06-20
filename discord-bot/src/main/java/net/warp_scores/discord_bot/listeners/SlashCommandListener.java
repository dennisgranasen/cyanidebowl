package net.warp_scores.discord_bot.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.commands.SlashCommand;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class SlashCommandListener {
    private final Collection<SlashCommand> commands;

    public SlashCommandListener(List<SlashCommand> slashCommands, GatewayDiscordClient client) {
        commands = slashCommands;

        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}




















    /*
    @RequiredArgsConstructor
    private enum BotAction {
        HELP("!help"),
        LATEST_MATCHES("!latestMatches"),
        LIVE_MATCHES("!liveMatches");

        @Getter
        private final String command;

        public static BotAction fromString(String commandName) {
            return Arrays.stream(values())
                    .filter(botAction -> botAction.command.equalsIgnoreCase(commandName))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    public Mono<Void> processCommand(Message eventMessage) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().startsWith("!"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> createMessageFor(channel, eventMessage.getContent()))
                .then();
    }

    private MessageCreateMono createMessageFor(MessageChannel channel, String commandName) {
        Optional<BotAction> action;
        try {
            action = Optional.ofNullable(BotAction.fromString(commandName));
        } catch (Exception ex) {
            log.error("Exception caught while determining action for command name '{}'.", commandName, ex);
            action = Optional.empty();
        }

        return action
                .map(a -> createMessageFor(channel, a))
                .orElse(createHelpMessage(channel, Optional.of(String.format("Unknown command '%s'...", commandName))));
    }

    private MessageCreateMono createMessageFor(MessageChannel channel, BotAction action) {
        switch (action) {
            case LATEST_MATCHES:
            case HELP:
                return createHelpMessage(channel, Optional.empty());
            case LIVE_MATCHES:
            default:
                return channel.createMessage(String.format("TODO: implement action for command '%s'.", action.name()));
        }
    }

    private MessageCreateMono createHelpMessage(MessageChannel channel, Optional<String> optionalErrorMessage) {

        return channel.createMessage()
    }
}
*/
