package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.inputs.MessageInputs;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

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
        final Member member = event.getMember().orElseThrow();
        if (!this.game.getPlayers().containsKey(member.getId())) {
            return Mono.just(false);
        }

        final String content = event.getMessage().getContent();
        if (!this.game.getActions().containsKey(content.toLowerCase())) {
            return Mono.just(false);
        }

        final Mono<Boolean> isLimited = this.game.getRateLimiter()
                .isLimitedAndWarn(event.getClient(), event.getGuildId().orElseThrow(), event.getMessage().getChannelId(),
                        event.getMember().orElseThrow().getId(), this.game.getContext().getLocale());
        return BooleanUtils.not(isLimited);
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return this.game.isScheduled();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        final Member member = event.getMember().orElseThrow();
        final Mono<MessageChannel> getChannel = event.getMessage().getChannel();

        final BlackjackPlayer player = this.game.getPlayers().get(member.getId());
        if (player.isStanding()) {
            return getChannel
                    .flatMap(channel -> DiscordUtil.sendMessage(Emoji.GREY_EXCLAMATION,
                            "(**%s**) You're standing, you can't play anymore.".formatted(member.getUsername()), channel))
                    .then();
        }

        final String content = event.getMessage().getContent();
        return Mono.
                defer(() -> {
                    if ("double down".equals(content) && player.getHand().count() != 2) {
                        return getChannel
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.GREY_EXCLAMATION,
                                        "(**%s**) You must have a maximum of 2 cards to use `double down`."
                                                .formatted(member.getUsername()), channel))
                                .then();
                    }

                    final Consumer<BlackjackPlayer> action = this.game.getActions().get(content);
                    if (action == null) {
                        return Mono.empty();
                    }

                    action.accept(player);

                    if (this.game.areAllPlayersStanding()) {
                        return this.game.end();
                    }
                    return this.game.show();
                })
                .then();
    }

}