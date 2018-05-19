package me.shadorc.shadbot.listener;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MemberListener {

	public static void onMemberJoin(MemberJoinEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuildId());

		sendAutoMsg(event.getGuild(), dbGuild.getMessageChannelId(), dbGuild.getJoinMessage());

		List<Role> autoRoles = dbGuild.getAutoRoles().stream()
				.map(Snowflake::of)
				.map(roleId -> event.getClient().getRoleById(dbGuild.getId(), roleId))
				.map(Mono<Role>::block)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if(BotUtils.hasPermissions(event.getGuild(), Permission.MANAGE_ROLES)
				&& BotUtils.canInteract(event.getGuild(), event.getMember())
				&& PermissionUtils.hasHierarchicalPermissions(event.getGuild(), event.getClient().getSelf(), roles)) {
			event.getGuild().editUserRoles(event.getUser(), autoRoles);
		}
	}

	public static void onMemberLeave(MemberLeaveEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getGuildId());
		sendAutoMsg(event.getClient().getGuildById(dbGuild.getId()),
				dbGuild.getMessageChannelId(),
				dbGuild.getLeaveMessage());
	}

	private static void sendAutoMsg(Mono<Guild> guild, Optional<Snowflake> channelId, Optional<String> msg) {
		if(!guild.hasElement().block() || !channelId.isPresent() || !msg.isPresent()) {
			return;
		}

		Flux<GuildChannel> channel = guild.flatMapMany(Guild::getChannels)
				.filter(chnl -> chnl.getId().equals(channelId.get()));

		if(!channel.hasElements().block()) {
			return;
		}

		BotUtils.sendMessage(msg.get(), (MessageChannel) channel.blockFirst());
	}
}
