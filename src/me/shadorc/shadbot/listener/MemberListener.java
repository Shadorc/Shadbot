package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MemberListener {

	public static Mono<Void> onMemberJoin(MemberJoinEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());
		return MemberListener.sendAutoMsg(event.getClient(), event.getMember(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage())
				// Add auto-role(s) to the new member
				.and(event.getGuild()
						.flatMap(guild -> Mono.justOrEmpty(event.getClient().getSelfId())
								.flatMap(selfId -> guild.getMemberById(selfId)))
						.flatMap(Member::getBasePermissions)
						.filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
						.flatMapMany(ignored -> Flux.fromIterable(dbGuild.getAutoRoles()))
						.map(Snowflake::of)
						.flatMap(roleId -> event.getMember().addRole(roleId)));

	}

	public static Mono<Void> onMemberLeave(MemberLeaveEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());
		return MemberListener.sendAutoMsg(event.getClient(), event.getUser(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage()).then();
	}

	private static Mono<Message> sendAutoMsg(DiscordClient client, User user, Optional<Long> channelId, Optional<String> message) {
		return Mono.zip(Mono.justOrEmpty(channelId).map(Snowflake::of), Mono.justOrEmpty(message))
				.flatMap(tuple -> client.getChannelById(tuple.getT1())
						.cast(MessageChannel.class)
						.flatMap(channel -> DiscordUtils.sendMessage(tuple.getT2().replace("{mention}", user.getMention()), channel)));
	};
}
