package com.shadorc.shadbot.object.inputs;

import com.shadorc.shadbot.object.ExceptionHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public abstract class MessageInputs {

    private final GatewayDiscordClient gateway;
    private final Duration timeout;
    private final Snowflake channelId;

    protected MessageInputs(GatewayDiscordClient gateway, Duration timeout, Snowflake channelId) {
        this.gateway = gateway;
        this.timeout = timeout;
        this.channelId = channelId;
    }

    public Flux<?> waitForInputs() {
        return this.gateway.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(event -> event.getMember().isPresent()
                        && !event.getMessage().getContent().isBlank()
                        && event.getMessage().getChannelId().equals(this.channelId))
                .takeWhile(this::takeEventWile)
                .filterWhen(this::isValidEvent)
                .flatMap(event -> this.processEvent(event)
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .take(this.timeout);
    }

    public Disposable listen() {
        return this.waitForInputs().subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * Evaluate each event against this predicate. If the predicate test succeeds, the event is
     * emitted. If the predicate test fails, the event is ignored.
     *
     * @param event The event to evaluate.
     * @return {@code true} if the event is valid and has to be processed, {@code false} otherwise.
     */
    public abstract Publisher<Boolean> isValidEvent(MessageCreateEvent event);

    /**
     * {@link MessageInputs#waitForInputs()} will relay events while this predicate returns {@code true} for
     * the event (checked before each event is delivered). This only includes the matching data.
     *
     * @param event The event.
     * @return {@code true} if the event has to be relayed, {@code false} if the {@link Flux}
     * has to stop emitting.
     */
    public abstract boolean takeEventWile(MessageCreateEvent event);

    /**
     * Process valid events.
     *
     * @param event The event to process.
     * @return A {@link Publisher} which emits when the event has been processed.
     */
    public abstract Mono<?> processEvent(MessageCreateEvent event);

}
