package net.warp_scores.discord_bot.services;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateMono;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class MessageListener {

    @RequiredArgsConstructor
    private enum BotAction {
        HELP("!help"),
        LAST_MATCHES("!lastMatches"),
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

    public MessageCreateMono createMessageFor(MessageChannel channel, String commandName) {
        BotAction action;
        try {
            action = BotAction.fromString(commandName);
        } catch (Exception ex) {
            return channel
                    .createMessage(String.format("I don't know, what '%s' means...", commandName));

        }

        switch (action) {
            case HELP:
                return createHelpMessageFor(channel);
            case LAST_MATCHES:
                return createLastMatchesMessageFor(channel);
            case LIVE_MATCHES:
                return createLiveMatchesMessageFor(channel);
            default:
                return MessageCreateMono.of(channel);
        }
    }

    public MessageCreateMono createHelpMessageFor(MessageChannel messageChannel) {
        return messageChannel.createMessage("TODO: implement help message");
    }

    public MessageCreateMono createLastMatchesMessageFor(MessageChannel messageChannel) {
        return messageChannel.createMessage("TODO: implement last matches message");
    }

    public MessageCreateMono createLiveMatchesMessageFor(MessageChannel messageChannel) {
        return messageChannel.createMessage("TODO: implement live matches message");
    }
}
