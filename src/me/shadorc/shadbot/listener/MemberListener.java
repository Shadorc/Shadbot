package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.utils.BotUtils;

public class MemberListener {

	public static void onMemberJoin(MemberJoinEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuildId());

		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage());

		// TODO: Implement and add doOnError(ExceptionUtils::isForbidden, err -> { ... })
		// List<Snowflake> autoRoles = dbGuild.getAutoRoles();
		// event.getGuild().editUserRoles(event.getMember(), autoRoles);
	}

	public static void onMemberLeave(MemberLeaveEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuildId());
		MemberListener.sendAutoMsg(event.getClient(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage());
	}

	private static void sendAutoMsg(DiscordClient client, Optional<Snowflake> channelId, Optional<String> msg) {
		if(!channelId.isPresent() || !msg.isPresent()) {
			return;
		}

		client.getMessageChannelById(channelId.get()).subscribe(channel -> BotUtils.sendMessage(msg.get(), channel));
	}
}
