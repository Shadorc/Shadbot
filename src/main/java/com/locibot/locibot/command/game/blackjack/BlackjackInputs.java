package com.locibot.locibot.command.game.blackjack;

import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.core.ratelimiter.RateLimitResponse;
import com.locibot.locibot.core.ratelimiter.RateLimiter;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.inputs.MessageInputs;
import com.locibot.locibot.object.message.TemporaryMessage;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;

public class BlackjackInputs extends MessageInputs {

    private final BlackjackGame game;

    private BlackjackInputs(GatewayDiscordClient gateway, BlackjackGame game) {
        super(gateway, game.getDuration(), game.getContext().getChannelId());
        this.game = game;
    }

    public static BlackjackInputs create(GatewayDiscordClient gateway, BlackjackGame game) {
        return new BlackjackInputs(gateway, game);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        final Snowflake memberId = event.getMember().orElseThrow().getId();
        if (!this.game.getPlayers().containsKey(memberId)) {
            return Mono.just(false);
        }

        final String content = event.getMessage().getContent();
        if (!this.game.getActions().containsKey(content.toLowerCase())) {
            return Mono.just(false);
        }

        final RateLimiter ratelimiter = this.game.getRateLimiter();
        final Snowflake guildId = event.getGuildId().orElseThrow();
        final RateLimitResponse response = ratelimiter.isLimited(guildId, memberId);
        if (response.shouldBeWarned()) {
            final Locale locale = this.game.getContext().getLocale();
            return new TemporaryMessage(event.getClient(), event.getMessage().getChannelId(), Duration.ofSeconds(8))
                    .send(Emoji.STOPWATCH, ratelimiter.formatRateLimitMessage(locale))
                    .thenReturn(!response.isLimited());
        }
        return Mono.just(!response.isLimited());
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return this.game.isScheduled();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final Member member = event.getMember().orElseThrow();
        final Mono<MessageChannel> getChannel = event.getMessage().getChannel();
        final Locale locale = this.game.getContext().getLocale();
        final Mono<Void> deleteMessage = event.getMessage().delete()
                .onErrorResume(err -> Mono.empty());

        final BlackjackPlayer player = this.game.getPlayers().get(member.getId());
        if (player.isStanding()) {
            return getChannel
                    .flatMap(channel -> DiscordUtil.sendMessage(Emoji.GREY_EXCLAMATION,
                            I18nManager.localize(locale, "blackjack.exception.standing")
                                    .formatted(member.getUsername()), channel))
                    .then();
        }

        final String content = event.getMessage().getContent();
        return Mono.
                defer(() -> {
                    if ("double down".equals(content) && player.getHand().count() != 2) {
                        return getChannel
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.GREY_EXCLAMATION,
                                        I18nManager.localize(locale, "blackjack.exception.double.down")
                                                .formatted(member.getUsername()), channel))
                                .then();
                    }

                    final Consumer<BlackjackPlayer> action = this.game.getActions().get(content);
                    if (action == null) {
                        return Mono.empty();
                    }

                    action.accept(player);

                    if (this.game.areAllPlayersStanding()) {
                        return deleteMessage
                                .then(this.game.end());
                    }
                    return deleteMessage
                            .then(this.game.show());
                })
                .then();
    }

}