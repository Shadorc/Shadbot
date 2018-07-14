package me.shadorc.shadbot.listener;

import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.SchedulerUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		LogUtils.infof("{Shard %d} %s",
				event.getClient().getConfig().getShardIndex(),
				event.toString());
	}

	public static void onReady(ReadyEvent event) {
		LogUtils.infof("Shadbot (Version: %s) is ready.", Config.VERSION);

		DiscordUtils.registerListener(event.getClient(), TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
		DiscordUtils.registerListener(event.getClient(), GuildCreateEvent.class, GuildListener::onGuildCreate);
		DiscordUtils.registerListener(event.getClient(), GuildDeleteEvent.class, GuildListener::onGuildDelete);
		DiscordUtils.registerListener(event.getClient(), MemberJoinEvent.class, MemberListener::onMemberJoin);
		DiscordUtils.registerListener(event.getClient(), MemberLeaveEvent.class, MemberListener::onMemberLeave);
		DiscordUtils.registerListener(event.getClient(), MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
		DiscordUtils.registerListener(event.getClient(), VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);

		SchedulerUtils.scheduleAtFixedRate(() -> NetUtils.postStats(event.getClient()), 2, 2, TimeUnit.HOURS);
		SchedulerUtils.scheduleAtFixedRate(() -> BotUtils.updatePresence(event.getClient()), 0, 30, TimeUnit.MINUTES);
	}

}