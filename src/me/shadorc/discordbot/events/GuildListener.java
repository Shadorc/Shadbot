package me.shadorc.discordbot.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;

@SuppressWarnings("ucd")
public class GuildListener {

	private final ExecutorService executor = Executors.newCachedThreadPool();

	@EventSubscriber
	public void onGuildEvent(GuildEvent event) {
		if(event instanceof GuildCreateEvent) {
			executor.execute(() -> this.onGuildCreate((GuildCreateEvent) event));
		} else if(event instanceof GuildLeaveEvent) {
			executor.execute(() -> this.onGuildLeave((GuildLeaveEvent) event));
		} else if(event instanceof UserJoinEvent) {
			executor.execute(() -> this.onUserJoin((UserJoinEvent) event));
		} else if(event instanceof UserLeaveEvent) {
			executor.execute(() -> this.onUserLeave((UserLeaveEvent) event));
		}
	}

	private void onGuildCreate(GuildCreateEvent event) {
		LogUtils.info("Shadbot connected to a guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	private void onGuildLeave(GuildLeaveEvent event) {
		LogUtils.info("Shadbot disconnected from guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	private void onUserJoin(UserJoinEvent event) {
		Long messageChannelID = (Long) DatabaseManager.getSetting(event.getGuild(), Setting.MESSAGE_CHANNEL_ID);
		String joinMessage = (String) DatabaseManager.getSetting(event.getGuild(), Setting.JOIN_MESSAGE);

		if(messageChannelID == null || joinMessage == null) {
			return;
		}

		IChannel messageChannel = event.getGuild().getChannelByID(messageChannelID);
		if(messageChannel == null) {
			return;
		}

		BotUtils.sendMessage(joinMessage, messageChannel);
	}

	private void onUserLeave(UserLeaveEvent event) {
		Long messageChannelID = (Long) DatabaseManager.getSetting(event.getGuild(), Setting.MESSAGE_CHANNEL_ID);
		String leaveMessage = (String) DatabaseManager.getSetting(event.getGuild(), Setting.LEAVE_MESSAGE);

		if(messageChannelID == null || leaveMessage == null) {
			return;
		}

		IChannel messageChannel = event.getGuild().getChannelByID(messageChannelID);
		if(messageChannel == null) {
			return;
		}

		BotUtils.sendMessage(leaveMessage, messageChannel);
	}
}
