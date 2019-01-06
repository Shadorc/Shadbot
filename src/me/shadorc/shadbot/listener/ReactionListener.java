package me.shadorc.shadbot.listener;

import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.admin.IamCmd;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.TemporaryMessage;
import reactor.core.publisher.Mono;

public class ReactionListener {

	private enum Action {
		ADD, REMOVE;
	}

	public static Mono<Void> onReactionAddEvent(ReactionAddEvent event) {
		return event.getMessage()
				.flatMap(message -> ReactionListener.iam(message, event.getUserId(), event.getEmoji(), Action.ADD));
	}

	public static Mono<Void> onReactionRemoveEvent(ReactionRemoveEvent event) {
		return event.getMessage()
				.flatMap(message -> ReactionListener.iam(message, event.getUserId(), event.getEmoji(), Action.REMOVE));
	}

	public static Mono<Void> iam(Message message, Snowflake userId, ReactionEmoji emoji, Action action) {
		final Function<Member, Mono<Boolean>> canManageRoles =
				member -> member.getBasePermissions()
						.flatMap(permission -> {
							if(!permission.contains(Permission.MANAGE_ROLES)) {
								return new TemporaryMessage(message.getClient(), message.getChannelId(), 15, ChronoUnit.SECONDS)
										.send(String.format(Emoji.ACCESS_DENIED
												+ " I can't add/remove a role due to a lack of permission."
												+ "%nPlease, check my permissions to verify that %s is checked.",
												String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
										.thenReturn(false);
							}
							return Mono.just(true);
						});

		return Mono.justOrEmpty(message.getClient().getSelfId())
				// It wasn't the bot that reacted
				.filter(selfId -> !userId.equals(selfId))
				// If this is the correct reaction
				.filter(selfId -> emoji.equals(IamCmd.REACTION))
				// If the bot is not the author of the message, this is not an Iam message
				.filterWhen(selfId -> Mono.justOrEmpty(message.getAuthorId()).map(selfId::equals))
				.flatMap(selfId -> message.getGuild().flatMap(guild -> guild.getMemberById(selfId)))
				.filterWhen(canManageRoles)
				.flatMap(member -> Mono.justOrEmpty(Shadbot.getDatabase()
						.getDBGuild(member.getGuildId())
						.getIamMessages()
						.get(message.getId().asString()))
						.map(Snowflake::of)
						.flatMap(roleId -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId)));
	}

}
