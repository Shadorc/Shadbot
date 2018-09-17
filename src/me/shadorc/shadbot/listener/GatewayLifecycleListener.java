package me.shadorc.shadbot.listener;

import java.time.Duration;

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
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		LogUtils.infof("{Shard %d} %s",
				event.getClient().getConfig().getShardIndex(),
				event.toString());
	}

	public static void onReady(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Config.VERSION);

		// TODO: Refactor this
		DiscordUtils.registerListener(event.getClient(), TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
		DiscordUtils.registerListener(event.getClient(), GuildCreateEvent.class, GuildListener::onGuildCreate);
		DiscordUtils.registerListener(event.getClient(), GuildDeleteEvent.class, GuildListener::onGuildDelete);
		DiscordUtils.registerListener(event.getClient(), MemberJoinEvent.class, MemberListener::onMemberJoin);
		DiscordUtils.registerListener(event.getClient(), MemberLeaveEvent.class, MemberListener::onMemberLeave);
		DiscordUtils.registerListener(event.getClient(), MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
		DiscordUtils.registerListener(event.getClient(), MessageUpdateEvent.class, MessageUpdateListener::onMessageUpdateEvent);
		DiscordUtils.registerListener(event.getClient(), VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);
		DiscordUtils.registerListener(event.getClient(), ReactionAddEvent.class, ReactionListener::onReactionAddEvent);
		DiscordUtils.registerListener(event.getClient(), ReactionRemoveEvent.class, ReactionListener::onReactionRemoveEvent);

		Flux.interval(Duration.ofHours(2), Duration.ofHours(2))
				.flatMap(ignored -> NetUtils.postStats(event.getClient()))
				.doOnError(err -> LogUtils.error(event.getClient(), err, "An error occurred while posting statistics."))
				.subscribe();

		Flux.interval(Duration.ZERO, Duration.ofMinutes(30))
				.flatMap(ignored -> BotUtils.updatePresence(event.getClient()))
				.doOnError(err -> LogUtils.error(event.getClient(), err, "An error occurred while updating presence."))
				.subscribe();
	}

}