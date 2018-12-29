package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Flux;

public class MemberListener {

	public static void onMemberJoin(MemberJoinEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());

		MemberListener.sendAutoMsg(event.getClient(), event.getMember(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage());

		// Add auto-role(s) to the new member
		event.getClient().getSelfId().ifPresent(selfId -> {
			event.getGuild()
					.flatMap(guild -> guild.getMemberById(selfId))
					.flatMap(Member::getBasePermissions)
					.filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
					.flatMapMany(ignored -> Flux.fromIterable(dbGuild.getAutoRoles()))
					.flatMap(roleId -> event.getMember().addRole(roleId))
					.subscribe();
		});

	}

	public static void onMemberLeave(MemberLeaveEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());
		MemberListener.sendAutoMsg(event.getClient(), event.getUser(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage());
	}

	private static void sendAutoMsg(DiscordClient client, User user, Optional<Snowflake> channelIdOpt, Optional<String> messageOpt) {
		channelIdOpt.ifPresent(channelId -> {
			messageOpt.ifPresent(message -> {
				client.getChannelById(channelId)
						.cast(MessageChannel.class)
						.flatMap(channel -> BotUtils.sendMessage(message.replace("{mention}", user.getMention()), channel))
						.subscribe();
			});
		});
	}
}
