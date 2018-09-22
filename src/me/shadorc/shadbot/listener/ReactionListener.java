package me.shadorc.shadbot.listener;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.command.admin.IamCommand;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.utils.object.message.TemporaryMessage;

public class ReactionListener {

	private enum Action {
		ADD, REMOVE;
	}

	public static void onReactionAddEvent(ReactionAddEvent event) {
		iam(event, event.getGuildId(), event.getChannelId(), event.getUserId(), event.getMessageId(), event.getEmoji(), Action.ADD);
	}

	public static void onReactionRemoveEvent(ReactionRemoveEvent event) {
		iam(event, event.getGuildId(), event.getChannelId(), event.getUserId(), event.getMessageId(), event.getEmoji(), Action.REMOVE);
	}

	public static void iam(MessageEvent event, Optional<Snowflake> guildIdOpt, Snowflake channelId, Snowflake userId,
			Snowflake messageId, ReactionEmoji reaction, Action action) {

		if(event.getClient().getSelfId().map(userId::equals).orElse(false)) {
			return;
		}

		guildIdOpt.ifPresent(guildId -> {
			final Snowflake roleId = DatabaseManager.getDBGuild(guildId)
					.getIamMessages()
					.get(messageId);
			if(roleId != null && reaction.equals(IamCommand.REACTION)) {
				event.getClient()
						.getMemberById(guildId, userId)
						.flatMap(member -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId))
						.onErrorResume(ExceptionHandler::isForbidden,
								err -> new TemporaryMessage(event.getClient(), channelId, 10, ChronoUnit.SECONDS)
										.send(String.format("I can't execute this command due to the lack of permission."
												+ "%nPlease, check my permissions and channel-specific ones to verify that %s is checked.",
												Permission.MANAGE_ROLES)))
						.subscribe();
			}
		});
	}

}
