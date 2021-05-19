package com.locibot.locibot.command.game.hangman;

import com.locibot.locibot.core.ratelimiter.RateLimitResponse;
import com.locibot.locibot.core.ratelimiter.RateLimiter;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.inputs.MessageInputs;
import com.locibot.locibot.object.message.TemporaryMessage;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
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

        final Mono<Boolean> checkRateLimit = Mono.
                defer(() -> {
                    final Snowflake guildId = event.getGuildId().orElseThrow();
                    final Snowflake memberId = event.getMember().orElseThrow().getId();
                    final RateLimiter rateLimiter = this.game.getRateLimiter();
                    final RateLimitResponse response = rateLimiter.isLimited(guildId, memberId);
                    if (response.shouldBeWarned()) {
                        final Locale locale = this.game.getContext().getLocale();
                        return new TemporaryMessage(event.getClient(), event.getMessage().getChannelId(), Duration.ofSeconds(8))
                                .send(Emoji.STOPWATCH, rateLimiter.formatRateLimitMessage(locale))
                                .thenReturn(response.isLimited());
                    }

                    return Mono.just(response.isLimited());
                })
                .filter(Boolean.FALSE::equals);

        final Mono<Void> deleteMessage = event.getMessage().delete()
                .onErrorResume(err -> Mono.empty());

        if (content.length() == 1) {
            return checkRateLimit.flatMap(__ -> deleteMessage
                    .then(this.game.checkLetter(content)));
        } else if (content.length() == this.game.getWord().length()) {
            return checkRateLimit.flatMap(__ -> deleteMessage
                    .then(this.game.checkWord(content)));
        }

        return Mono.empty();
    }

}
