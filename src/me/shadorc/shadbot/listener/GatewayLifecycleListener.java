package me.shadorc.shadbot.listener;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.Presence;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		Optional<Integer> shardIndex = Optional.ofNullable(event.getClient().getConfig().getShardIndex());
		LogUtils.infof("[Shard %d] %s", shardIndex.orElse(0), event.toString());
	}

	// TODO
	// public static void onShardReadyEvent(ShardReadyEvent event) {
	// ShardManager.addShardIfAbsent(event.getShard());
	// }

	// TODO
	// public static void onResumEvent(ResumeEvent event) {
	// ShardManager.getShadbotShard(event.getShard()).sendQueue();
	// }

	public static void onReady(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Config.VERSION);

		// TODO: This should probably not be initialized here
		// Ready event is launched every time a shard is ready or just when the bot is ready ?
		ShardManager.start();

		Shadbot.getScheduler().scheduleAtFixedRate(() -> LottoCmd.draw(), LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);
		Shadbot.getScheduler().scheduleAtFixedRate(() -> BotUtils.updatePresence(), 1, 30, TimeUnit.MINUTES);
		Shadbot.getScheduler().scheduleAtFixedRate(() -> NetUtils.postStats(), 2, 2, TimeUnit.HOURS);

		Shadbot.registerListener(event.getClient(), TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
		Shadbot.registerListener(event.getClient(), GuildCreateEvent.class, GuildListener::onGuildCreate);
		Shadbot.registerListener(event.getClient(), GuildDeleteEvent.class, GuildListener::onGuildDelete);
		Shadbot.registerListener(event.getClient(), MemberJoinEvent.class, MemberListener::onMemberJoin);
		Shadbot.registerListener(event.getClient(), MemberLeaveEvent.class, MemberListener::onMemberLeave);
		Shadbot.registerListener(event.getClient(), MessageCreateEvent.class, MessageListener::onMessageCreate);

		// new UserVoiceChannelListener(),
		// new VoiceChannelListener());

		event.getClient().updatePresence(Presence.online());
	}

}