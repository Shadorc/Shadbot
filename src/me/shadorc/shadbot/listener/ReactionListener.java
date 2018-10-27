package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.command.admin.IamCommand;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class ReactionListener {

	public static class ReactionEvent {
		private final ReactionAddEvent reactionAddEvent;
		private final ReactionRemoveEvent reactionRemoveEvent;

		public ReactionEvent(ReactionAddEvent reactionAddEvent) {
			this.reactionAddEvent = reactionAddEvent;
			this.reactionRemoveEvent = null;
		}

		public ReactionEvent(ReactionRemoveEvent reactionRemoveEvent) {
			this.reactionRemoveEvent = reactionRemoveEvent;
			this.reactionAddEvent = null;
		}

		private DiscordClient getClient() {
			return reactionAddEvent == null ? reactionRemoveEvent.getClient() : reactionAddEvent.getClient();
		}

		public Mono<MessageChannel> getChannel() {
			return reactionAddEvent == null ? reactionRemoveEvent.getChannel() : reactionAddEvent.getChannel();
		}

		public Optional<Snowflake> getSelfId() {
			return this.getClient().getSelfId();
		}

		public Optional<Snowflake> getGuildId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getGuildId() : reactionAddEvent.getGuildId();
		}

		public Snowflake getUserId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getUserId() : reactionAddEvent.getUserId();
		}

		public Snowflake getMessageId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getMessageId() : reactionAddEvent.getMessageId();
		}

		public ReactionEmoji getEmoji() {
			return reactionAddEvent == null ? reactionRemoveEvent.getEmoji() : reactionAddEvent.getEmoji();
		}

	}

	private enum Action {
		ADD, REMOVE;
	}

	public static void onReactionAddEvent(ReactionAddEvent event) {
		iam(new ReactionEvent(event), Action.ADD);
	}

	public static void onReactionRemoveEvent(ReactionRemoveEvent event) {
		iam(new ReactionEvent(event), Action.REMOVE);
	}

	public static void iam(ReactionEvent event, Action action) {

		// If the bot is not the author of the message, this is not an Iam message
		if(event.getSelfId().map(event.getUserId()::equals).orElse(false)) {
			return;
		}

		event.getGuildId().ifPresent(guildId -> {
			final Snowflake roleId = Optional.ofNullable(DatabaseManager.getDBGuild(guildId)
					.getIamMessages()
					.get(event.getMessageId().asLong()))
					.map(Snowflake::of)
					.orElse(null);

			if(roleId != null && event.getEmoji().equals(IamCommand.REACTION)) {
				event.getClient()
						.getMemberById(guildId, event.getUserId())
						.flatMap(member -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId))
						.onErrorResume(ExceptionHandler::isForbidden,
								err -> BotUtils.sendMessage(
										String.format(Emoji.ACCESS_DENIED
												+ " I can't add/remove a role due to the lack of permission."
												+ "%nPlease, check my permissions to verify that %s is checked.",
												String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))),
										event.getChannel())
										.then())
						.subscribe();
			}
		});
	}

}
