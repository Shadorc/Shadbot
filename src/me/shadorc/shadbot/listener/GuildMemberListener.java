package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.GuildMemberEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class GuildMemberListener {

	@EventSubscriber
	public void onGuildMemberEvent(GuildMemberEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			if(event instanceof UserJoinEvent) {
				this.onUserJoinEvent((UserJoinEvent) event);
			} else if(event instanceof UserLeaveEvent) {
				this.onUserLeaveEvent((UserLeaveEvent) event);
			}
		});
	}

	private void onUserJoinEvent(UserJoinEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuild());
		Long channelID = dbGuild.getMessageChannelID();
		String leaveMsg = dbGuild.getLeaveMessage();
		this.sendAutoMsg(event.getGuild(), channelID, leaveMsg);
	}

	private void onUserLeaveEvent(UserLeaveEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuild());
		Long messageChannelID = dbGuild.getMessageChannelID();
		String joinMessage = dbGuild.getJoinMessage();
		this.sendAutoMsg(event.getGuild(), messageChannelID, joinMessage);
	}

	private void sendAutoMsg(IGuild guild, Long channelID, String msg) {
		if(channelID == null || msg == null) {
			return;
		}

		IChannel messageChannel = guild.getChannelByID(channelID);
		if(messageChannel == null) {
			return;
		}

		BotUtils.sendMessage(msg, messageChannel);
	}
}
