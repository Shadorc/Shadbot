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
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.CustomShard;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		Optional<Integer> shardIndex = Optional.ofNullable(event.getClient().getConfig().getShardIndex());
		LogUtils.infof("{Shard %d} %s", shardIndex.orElse(0), event.toString());
	}

	public static void onReady(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Config.VERSION);

		Shadbot.registerListener(event.getClient(), TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
		Shadbot.registerListener(event.getClient(), GuildCreateEvent.class, GuildListener::onGuildCreate);
		Shadbot.registerListener(event.getClient(), GuildDeleteEvent.class, GuildListener::onGuildDelete);
		Shadbot.registerListener(event.getClient(), MemberJoinEvent.class, MemberListener::onMemberJoin);
		Shadbot.registerListener(event.getClient(), MemberLeaveEvent.class, MemberListener::onMemberLeave);
		Shadbot.registerListener(event.getClient(), MessageCreateEvent.class, MessageListener::onMessageCreate);
		// TODO: UserVoiceChannelListener, VoiceChannelListener

		new CustomShard(event.getClient());

		Shadbot.scheduleAtFixedRate(() -> BotUtils.updatePresence(event.getClient()), 0, 30, TimeUnit.MINUTES);
	}

}