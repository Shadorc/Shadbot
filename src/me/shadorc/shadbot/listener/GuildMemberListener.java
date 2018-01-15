package me.shadorc.shadbot.listener;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

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
		String joinMsg = dbGuild.getJoinMessage();
		this.sendAutoMsg(event.getGuild(), channelID, joinMsg);

		List<IRole> roles = dbGuild.getAutoRoles().stream()
				.map(event.getGuild()::getRoleByID)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if(PermissionUtils.hasPermissions(event.getGuild(), event.getClient().getOurUser(), Permissions.MANAGE_ROLES)) {
			event.getGuild().editUserRoles(event.getUser(), roles.toArray(new IRole[roles.size()]));
		}
	}

	private void onUserLeaveEvent(UserLeaveEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuild());

		Long channelID = dbGuild.getMessageChannelID();
		String leaveMsg = dbGuild.getLeaveMessage();
		this.sendAutoMsg(event.getGuild(), channelID, leaveMsg);
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
