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
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MemberListener {

	public static Mono<Void> onMemberJoin(MemberJoinEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());
		return Mono.just(MemberListener.sendAutoMsg(event.getClient(), event.getMember(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage()))
				// Add auto-role(s) to the new member
				.then(event.getGuild())
				.flatMap(guild -> Mono.justOrEmpty(event.getClient().getSelfId())
						.flatMap(selfId -> guild.getMemberById(selfId)))
				.flatMap(Member::getBasePermissions)
				.filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
				.flatMapMany(ignored -> Flux.fromIterable(dbGuild.getAutoRoles()))
				.flatMap(roleId -> event.getMember().addRole(roleId))
				.then();

	}

	public static Mono<Message> onMemberLeave(MemberLeaveEvent event) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId());
		return MemberListener.sendAutoMsg(event.getClient(), event.getUser(), dbGuild.getMessageChannelId(), dbGuild.getLeaveMessage());
	}

	private static Mono<Message> sendAutoMsg(DiscordClient client, User user, Optional<Snowflake> channelId, Optional<String> message) {
		return Mono.zip(Mono.justOrEmpty(channelId), Mono.justOrEmpty(message))
				.flatMap(tuple -> client.getChannelById(tuple.getT1())
						.cast(MessageChannel.class)
						.flatMap(channel -> BotUtils.sendMessage(tuple.getT2().replace("{mention}", user.getMention()), channel)));
	};
}
