package me.shadorc.shadbot.listener;

import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.admin.IamCmd;
import me.shadorc.shadbot.utils.DiscordUtils;
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
		final Function<? super MessageChannel, ? extends Publisher<Boolean>> canManageRoles =
				channel -> DiscordUtils.hasPermission(channel, channel.getClient().getSelfId().get(), Permission.MANAGE_ROLES)
						.flatMap(hasPerm -> {
							if(!hasPerm) {
								return new TemporaryMessage(channel.getClient(), channel.getId(), 15, ChronoUnit.SECONDS)
										.send(String.format(Emoji.ACCESS_DENIED
												+ " I can't add/remove a role due to a lack of permission."
												+ "%nPlease, check my permissions to verify that %s is checked.",
												String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
										.thenReturn(hasPerm);
							}
							return Mono.just(hasPerm);
						});

		return Mono.justOrEmpty(message.getClient().getSelfId())
				// It wasn't the bot that reacted
				.filter(selfId -> !userId.equals(selfId))
				// If this is the correct reaction
				.filter(ignored -> emoji.equals(IamCmd.REACTION))
				// If the bot is not the author of the message, this is not an Iam message
				.filterWhen(selfId -> Mono.justOrEmpty(message.getAuthorId()).map(selfId::equals))
				.flatMap(selfId -> message.getGuild().map(Guild::getId)
						.flatMap(guildId -> Mono.justOrEmpty(Shadbot.getDatabase().getDBGuild(guildId).getIamMessages().get(message.getId().asString()))))
				.map(Snowflake::of)
				// If the bot can manage roles
				.flatMap(roleId -> message.getChannel().filterWhen(canManageRoles).map(ignored -> roleId))
				.flatMap(roleId -> message.getGuild().flatMap(guild -> guild.getMemberById(userId))
						.flatMap(member -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId)));
	}

}
