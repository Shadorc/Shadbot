package me.shadorc.shadbot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.listener.ChannelListener;
import me.shadorc.shadbot.listener.GatewayLifecycleListener;
import me.shadorc.shadbot.listener.GuildListener;
import me.shadorc.shadbot.listener.MemberListener;
import me.shadorc.shadbot.listener.MessageCreateListener;
import me.shadorc.shadbot.listener.MessageUpdateListener;
import me.shadorc.shadbot.listener.ReactionListener;
import me.shadorc.shadbot.listener.VoiceStateUpdateListener;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Shadbot {

	private static final Instant LAUNCH_TIME = Instant.now();
	private static final List<DiscordClient> CLIENTS = new ArrayList<>();

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);

		// If file loading or command generation has failed, abort attempt to connect the bot
		if(!DataManager.init() || !CommandManager.init()) {
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(DataManager::stop));

		final int shardCount = 1;
		final DiscordClientBuilder builder = new DiscordClientBuilder(APIKeys.get(APIKey.DISCORD_TOKEN))
				.setShardCount(shardCount)
				.setInitialPresence(Presence.idle(Activity.playing("Connecting...")));

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(builder.getShardCount(), "shard"));
		for(int i = 0; i < builder.getShardCount(); i++) {
			final DiscordClient client = builder.setShardIndex(i).build();
			CLIENTS.add(client);

			final EventDispatcher dispatcher = client.getEventDispatcher();
			dispatcher.on(ReadyEvent.class).subscribe(GatewayLifecycleListener::onReadyEvent);
			dispatcher.on(GatewayLifecycleEvent.class).subscribe(GatewayLifecycleListener::onGatewayLifecycleEvent);
			dispatcher.on(TextChannelDeleteEvent.class).subscribe(ChannelListener::onTextChannelDelete);
			dispatcher.on(GuildCreateEvent.class).subscribe(GuildListener::onGuildCreate);
			dispatcher.on(GuildDeleteEvent.class).subscribe(GuildListener::onGuildDelete);
			dispatcher.on(MemberJoinEvent.class).subscribe(MemberListener::onMemberJoin);
			dispatcher.on(MemberLeaveEvent.class).subscribe(MemberListener::onMemberLeave);
			dispatcher.on(MessageCreateEvent.class).subscribe(MessageCreateListener::onMessageCreate);
			dispatcher.on(MessageUpdateEvent.class).subscribe(MessageUpdateListener::onMessageUpdateEvent);
			dispatcher.on(VoiceStateUpdateEvent.class).subscribe(VoiceStateUpdateListener::onVoiceStateUpdateEvent);
			dispatcher.on(ReactionAddEvent.class).subscribe(ReactionListener::onReactionAddEvent);
			dispatcher.on(ReactionRemoveEvent.class).subscribe(ReactionListener::onReactionRemoveEvent);
		}

		Flux.interval(LottoCmd.getDelay(), Duration.ofDays(7))
				.doOnNext(ignored -> LottoCmd.draw(CLIENTS.get(0)))
				.subscribe();

		// Initiate login and block
		Mono.when(CLIENTS.stream().map(DiscordClient::login).collect(Collectors.toList())).block();
	}

	public static void logout() {
		CLIENTS.forEach(DiscordClient::logout);
		System.exit(0);
	}

	/**
	 * @return The time when this class was loaded.
	 */
	public static Instant getLaunchTime() {
		return LAUNCH_TIME;
	}

}
