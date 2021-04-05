package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.object.inputs.MessageInputs;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class HangmanInputs extends MessageInputs {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z]+");

    private final HangmanGame game;

    private HangmanInputs(GatewayDiscordClient gateway, HangmanGame game) {
        super(gateway, game.getDuration(), game.getContext().getChannelId());
        this.game = game;
    }

    public static HangmanInputs create(GatewayDiscordClient gateway, HangmanGame game) {
        return new HangmanInputs(gateway, game);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMember())
                .map(Member::getId)
                .map(this.game.getPlayers()::containsKey);
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return this.game.isScheduled();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final String content = event.getMessage().getContent().toLowerCase().trim();

        // Check only if content is an unique word/letter
        if (!WORD_PATTERN.matcher(content).matches()) {
            return Mono.empty();
        }

        final Mono<Boolean> checkRateLimit = this.game.getRateLimiter().isLimitedAndWarn(
                event.getClient(), event.getGuildId().orElseThrow(), event.getMessage().getChannelId(),
                event.getMember().orElseThrow().getId(), this.game.getContext().getLocale())
                .filter(Boolean.FALSE::equals);

        if (content.length() == 1) {
            return checkRateLimit.flatMap(__ -> this.game.checkLetter(content));
        } else if (content.length() == this.game.getWord().length()) {
            return checkRateLimit.flatMap(__ -> this.game.checkWord(content));
        }

        return Mono.empty();
    }

}
