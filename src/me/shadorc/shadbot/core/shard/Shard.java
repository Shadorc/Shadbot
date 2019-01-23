package me.shadorc.shadbot.core.shard;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ConnectEvent;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ReconnectEvent;
import discord4j.core.event.domain.lifecycle.ReconnectFailEvent;
import discord4j.core.event.domain.lifecycle.ReconnectStartEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.gateway.retry.GatewayStateChange.State;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.ChannelListener;
import me.shadorc.shadbot.listener.GuildListener;
import me.shadorc.shadbot.listener.MemberListener;
import me.shadorc.shadbot.listener.MessageCreateListener;
import me.shadorc.shadbot.listener.MessageUpdateListener;
import me.shadorc.shadbot.listener.ReactionListener;
import me.shadorc.shadbot.listener.ReadyListener;
import me.shadorc.shadbot.listener.VoiceStateUpdateListener;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.publisher.Mono;

public class Shard {

	private final DiscordClient client;
	private final AtomicBoolean isFullyReady;

	private volatile State state;

	public Shard(DiscordClient client) {
		this.client = client;
		this.isFullyReady = new AtomicBoolean(false);

		this.register(GatewayLifecycleEvent.class, this::onGatewayLifecycleEvent);
		this.register(ReadyEvent.class, ReadyListener::onReadyEvent);
		this.register(TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
		this.register(GuildCreateEvent.class, GuildListener::onGuildCreate);
		this.register(GuildDeleteEvent.class, GuildListener::onGuildDelete);
		this.register(MemberJoinEvent.class, MemberListener::onMemberJoin);
		this.register(MemberLeaveEvent.class, MemberListener::onMemberLeave);
		this.register(MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
		this.register(MessageUpdateEvent.class, MessageUpdateListener::onMessageUpdateEvent);
		this.register(VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);
		this.register(ReactionAddEvent.class, ReactionListener::onReactionAddEvent);
		this.register(ReactionRemoveEvent.class, ReactionListener::onReactionRemoveEvent);
		this.registerFullyReadyEvent();
	}

	private <T extends Event> void register(Class<T> eventClass, Function<T, Mono<Void>> mapper) {
		this.getClient().getEventDispatcher()
				.on(eventClass)
				.flatMap(event -> mapper.apply(event)
						.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.client, err))))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	private void registerFullyReadyEvent() {
		this.getClient().getEventDispatcher()
				.on(ReadyEvent.class)
				.map(event -> event.getGuilds().size())
				.flatMap(size -> this.client.getEventDispatcher()
						.on(GuildCreateEvent.class)
						.take(size)
						.last())
				.flatMap(event -> {
					this.isFullyReady.set(true);
					return Shadbot.onFullyReadyEvent(event)
							.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.client, err)));
				})
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	private Mono<Void> onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		return Mono.fromRunnable(() -> {
			if(event instanceof ConnectEvent) {
				this.state = State.CONNECTED;
			} else if(event instanceof DisconnectEvent) {
				this.state = State.DISCONNECTED;
			} else if(event instanceof ReconnectFailEvent) {
				this.state = State.RETRY_FAILED;
			} else if(event instanceof ReconnectStartEvent) {
				this.state = State.RETRY_STARTED;
			} else if(event instanceof ReconnectEvent) {
				this.state = State.RETRY_SUCCEEDED;
			}
			
			switch(this.state) {
				case RETRY_SUCCEEDED:
					this.isFullyReady.set(true);
					break;
				default:
					this.isFullyReady.set(false);
					break;
			}
		});
	}

	public DiscordClient getClient() {
		return this.client;
	}

	public State getState() {
		return this.state;
	}

	public boolean isFullyReady() {
		return this.isFullyReady.get();
	}

}
