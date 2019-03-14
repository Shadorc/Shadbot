package me.shadorc.shadbot.listener.interceptor;

import java.time.Duration;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class Inputs {

	private final DiscordClient client;
	private final Duration timeout;

	public Inputs(DiscordClient client, Duration timeout) {
		this.client = client;
		this.timeout = timeout;
	}

	public final Flux<Void> waitForInputs() {
		return this.client.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.takeWhile(this::takeEventWile)
				.filterWhen(this::isValidEvent)
				.flatMap(this::processEvent)
				.take(this.timeout);
	}

	/**
	 * Evaluate each event against this predicate. If the predicate test succeeds, the event is
	 * emitted. If the predicate test fails, the event is ignored.
	 * 
	 * @param event - the event
	 * @return {@code true} if the event is valid and has to be processed, {code false} otherwise.
	 */
	public abstract Mono<Boolean> isValidEvent(MessageCreateEvent event);

	/**
	 * {@code waitForInput} will relay events while this predicate returns {@code true} for
	 * the event (checked before each event is delivered). This only includes the matching data.
	 * 
	 * @param event - the event
	 * @return {@code true} if the event has to be relayed, {@code false} if the {@link Flux}
	 *         has to stop emitting.
	 */
	public abstract boolean takeEventWile(MessageCreateEvent event);

	/**
	 * Process valid events.
	 * 
	 * @param event - the event
	 * @return A {@link Mono} which emits when the event has been processed.
	 */
	public abstract Mono<Void> processEvent(MessageCreateEvent event);

}
