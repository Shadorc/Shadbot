package me.shadorc.shadbot.listener;

import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.PermissionUtils;
import sx.blah.discord.util.RequestBuffer;

public class GuildMemberListener {

	public static class MemberJoinListener implements Consumer<MemberJoinEvent> {

		@Override
		public void accept(MemberJoinEvent event) {
			Guild guild = event.getGuild().block();
			DBGuild dbGuild = Database.getDBGuild(guild);

			Long channelID = dbGuild.getMessageChannelID();
			String joinMsg = dbGuild.getJoinMessage();
			sendAutoMsg(guild, channelID, joinMsg);

			List<Long> autoRoles = dbGuild.getAutoRoles();

			List<Role> roles = guild.getRoles()
					.filter(role -> autoRoles.contains(role.getId().asLong()))
					.collect(Collectors.toList())
					.block();

			if(BotUtils.hasPermissions(event.getGuild(), Permission.MANAGE_ROLES)
					&& BotUtils.canInteract(event.getGuild(), event.getMember())
					&& PermissionUtils.hasHierarchicalPermissions(event.getGuild(), event.getClient().getOurUser(), roles)) {
				RequestBuffer.request(() -> {
					event.getGuild().editUserRoles(event.getUser(), roles.toArray(new IRole[roles.size()]));
				});
			}
		}
	}

	public static class MemberLeaverListener implements Consumer<MemberLeaveEvent> {

		@Override
		public void accept(MemberLeaveEvent event) {
			Guild guild = event.getClient().getGuildById(event.getGuildId()).block();
			DBGuild dbGuild = Database.getDBGuild(guild);
			sendAutoMsg(guild, dbGuild.getMessageChannelID(), dbGuild.getLeaveMessage());
		}

	}

	private static void sendAutoMsg(Guild guild, Long channelID, String msg) {
		if(channelID == null || msg == null) {
			return;
		}

		GuildChannel messageChannel = guild.getChannels().filter(channel -> channel.getId().equals(Snowflake.of(channelID))).blockFirst();
		if(messageChannel == null) {
			return;
		}

		BotUtils.sendMessage(msg, messageChannel);
	}
}
