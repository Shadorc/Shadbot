package me.shadorc.discordbot.events;

import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.data.StorageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;

@SuppressWarnings("ucd")
public class GuildListener {

	@EventSubscriber
	public void onGuildCreateEvent(GuildCreateEvent event) {
		LogUtils.info("Shadbot connected to a guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onGuildLeaveEvent(GuildLeaveEvent event) {
		LogUtils.info("Shadbot disconnected from guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	@EventSubscriber
	public void onUserJoinEvent(UserJoinEvent event) {
		Long messageChannelID = (Long) StorageManager.getSetting(event.getGuild(), Setting.MESSAGE_CHANNEL_ID);
		String joinMessage = (String) StorageManager.getSetting(event.getGuild(), Setting.JOIN_MESSAGE);

		if(messageChannelID == null || joinMessage == null) {
			return;
		}

		IChannel messageChannel = event.getGuild().getChannelByID(messageChannelID);
		if(messageChannel == null) {
			return;
		}

		BotUtils.sendMessage(joinMessage, messageChannel);
	}

	@EventSubscriber
	public void onUserLeaveEvent(UserLeaveEvent event) {
		Long messageChannelID = (Long) StorageManager.getSetting(event.getGuild(), Setting.MESSAGE_CHANNEL_ID);
		String leaveMessage = (String) StorageManager.getSetting(event.getGuild(), Setting.LEAVE_MESSAGE);

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
