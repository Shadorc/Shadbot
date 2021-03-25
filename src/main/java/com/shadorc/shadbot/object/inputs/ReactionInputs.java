package com.shadorc.shadbot.object.inputs;

import com.shadorc.shadbot.object.ExceptionHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public abstract class ReactionInputs {

    private final GatewayDiscordClient gateway;
    private final Snowflake messageId;
    private final ReactionEmoji reactionEmoji;
    private final Duration timeout;

    public ReactionInputs(GatewayDiscordClient gateway, Snowflake messageId, ReactionEmoji reactionEmoji, Duration timeout) {
        this.gateway = gateway;
        this.messageId = messageId;
        this.reactionEmoji = reactionEmoji;
        this.timeout = timeout;
    }

    public Flux<?> waitForInputs() {
        return this.gateway.getEventDispatcher()
                .on(ReactionAddEvent.class)
                .filter(event -> event.getMessageId().equals(this.messageId)
                        && event.getEmoji().equals(this.reactionEmoji))
                .takeWhile(this::takeEventWile)
                .flatMap(event -> this.onReactionAddEvent(event)
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .take(this.timeout);
    }

    /**
     * {@link ReactionInputs#waitForInputs()} will relay events while this predicate returns {@code true} for
     * the event (checked before each event is delivered). This only includes the matching data.
     *
     * @param event The event.
     * @return {@code true} if the event has to be relayed, {@code false} if the {@link Flux}
     * has to stop emitting.
     */
    public abstract boolean takeEventWile(ReactionAddEvent event);

    public abstract Mono<?> onReactionAddEvent(ReactionAddEvent event);

}
