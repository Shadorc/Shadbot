package me.shadorc.discordbot.events;

import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.GuildMemberEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;

@SuppressWarnings("ucd")
public class GuildMemberListener {

	@EventSubscriber
	public void onGuildMemberEvent(GuildMemberEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			long startTime = System.currentTimeMillis();
			if(event instanceof UserJoinEvent) {
				this.onUserJoinEvent((UserJoinEvent) event);
			} else if(event instanceof UserLeaveEvent) {
				this.onUserLeaveEvent((UserLeaveEvent) event);
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			if(elapsedTime / 1000 > 10) {
				LogUtils.info("{DEBUG} GuildMemberListener | Long event detected !"
						+ "\nDuration: " + elapsedTime
						+ "\nEvent: " + event);
			}
		});
	}

	private void onUserJoinEvent(UserJoinEvent event) {
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

	private void onUserLeaveEvent(UserLeaveEvent event) {
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
