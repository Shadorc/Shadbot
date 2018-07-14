package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;

public class MemberListener {

	public static void onMemberJoin(MemberJoinEvent event) {
		final Snowflake guildId = event.getGuildId();
		final DBGuild dbGuild = DatabaseManager.getDBGuild(guildId);

		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage());

		Flux.fromIterable(dbGuild.getAutoRoles())
				.flatMap(roleId -> event.getMember().addRole(roleId))
				.doOnError(ExceptionHandler::isForbidden,
						err -> LogUtils.infof("{Guild ID: %d} Shadbot was not allowed to edit role.", guildId.asLong()))
				.doOnError(ExceptionHandler::isNotForbidden,
						err -> LogUtils.error(event.getClient(), err, String.format("{Guild ID: %d} An error occurred while editing a role.", guildId.asLong())))
				.subscribe();
	}

	public static void onMemberLeave(MemberLeaveEvent event) {
		final DBGuild dbGuild = DatabaseManager.getDBGuild(event.getGuildId());
		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage());
	}

	private static void sendAutoMsg(DiscordClient client, Optional<Snowflake> channelId, Optional<String> msg) {
		if(channelId.isPresent() && msg.isPresent()) {
			BotUtils.sendMessage(msg.get(), client.getMessageChannelById(channelId.get())).subscribe();
		}
	}
}
