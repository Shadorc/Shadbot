package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;

public class MemberListener {

	public static void onMemberJoin(MemberJoinEvent event) {
		final DBGuild dbGuild = DatabaseManager.getDBGuild(event.getGuildId());

		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage());

		Flux.fromIterable(dbGuild.getAutoRoles())
				.flatMap(roleId -> event.getMember().addRole(roleId))
				.doOnError(ExceptionHandler::isForbidden,
						err -> LogUtils.cannot(MemberListener.class, event.getGuildId(), Permission.MANAGE_ROLES))
				.subscribe();
	}

	public static void onMemberLeave(MemberLeaveEvent event) {
		final DBGuild dbGuild = DatabaseManager.getDBGuild(event.getGuildId());
		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage());
	}

	private static void sendAutoMsg(DiscordClient client, Optional<Snowflake> channelId, Optional<String> message) {
		if(channelId.isPresent() && message.isPresent()) {
			BotUtils.sendMessage(message.get(), client.getMessageChannelById(channelId.get()))
					.doOnError(ExceptionHandler::isForbidden, err -> LogUtils.cannotSpeak(MemberListener.class))
					.subscribe();
		}
	}
}
