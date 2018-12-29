package me.shadorc.shadbot.listener;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.admin.IamCmd;
import me.shadorc.shadbot.core.exception.ExceptionHandler;
import me.shadorc.shadbot.core.exception.ExceptionUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.TemporaryMessage;
import reactor.core.publisher.Mono;

public class ReactionListener {

	private static class ReactionEvent {
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
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getClient() : this.reactionAddEvent.getClient();
		}

		private Mono<Message> getMessage() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getMessage() : this.reactionAddEvent.getMessage();
		}

		private Mono<MessageChannel> getChannel() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getChannel() : this.reactionAddEvent.getChannel();
		}

		private Snowflake getSelfId() {
			return this.getClient().getSelfId().orElse(null);
		}

		private Mono<Member> getMember() {
			return this.getGuildId()
					.map(guildId -> this.getClient().getMemberById(guildId, this.getUserId()))
					.orElse(Mono.empty());
		}

		private Mono<String> getUsername() {
			return this.getMember().map(Member::getUsername);
		}

		private Snowflake getChannelId() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getChannelId() : this.reactionAddEvent.getChannelId();
		}

		private Optional<Snowflake> getGuildId() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getGuildId() : this.reactionAddEvent.getGuildId();
		}

		private Snowflake getUserId() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getUserId() : this.reactionAddEvent.getUserId();
		}

		private Snowflake getMessageId() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getMessageId() : this.reactionAddEvent.getMessageId();
		}

		private ReactionEmoji getEmoji() {
			return this.reactionAddEvent == null ? this.reactionRemoveEvent.getEmoji() : this.reactionAddEvent.getEmoji();
		}

	}

	private enum Action {
		ADD, REMOVE;
	}

	public static void onReactionAddEvent(ReactionAddEvent event) {
		ReactionListener.iam(new ReactionEvent(event), Action.ADD);
	}

	public static void onReactionRemoveEvent(ReactionRemoveEvent event) {
		ReactionListener.iam(new ReactionEvent(event), Action.REMOVE);
	}

	public static void iam(ReactionEvent event, Action action) {
		Mono.justOrEmpty(event.getSelfId())
				// It wasn't the bot that reacted
				.filter(selfId -> !event.getUserId().equals(selfId))
				.filter(ignored -> event.getEmoji().equals(IamCmd.REACTION))
				// If the bot is not the author of the message, this is not an Iam message
				.filterWhen(selfId -> event.getMessage()
						.map(Message::getAuthorId)
						.flatMap(Mono::justOrEmpty)
						.map(selfId::equals))
				.flatMap(selfId -> Mono.justOrEmpty(event.getGuildId()))
				.flatMap(guildId -> Mono.justOrEmpty(Shadbot.getDatabase().getDBGuild(guildId)
						.getIamMessages()
						.get(event.getMessageId().asString())))
				.map(Snowflake::of)
				.filterWhen(roleId -> event.getChannel()
						.flatMap(channel -> DiscordUtils.hasPermission(channel, event.getSelfId(), Permission.MANAGE_ROLES)
								.flatMap(hasPerm -> {
									if(!hasPerm) {
										return new TemporaryMessage(event.getClient(), event.getChannelId(), 15, ChronoUnit.SECONDS)
												.send(String.format(Emoji.ACCESS_DENIED
														+ " I can't add/remove a role due to a lack of permission."
														+ "%nPlease, check my permissions to verify that %s is checked.",
														String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
												.thenReturn(hasPerm);
									}
									return Mono.just(hasPerm);
								})))
				.flatMap(roleId -> event.getMember()
						.flatMap(member -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId)))
				.onErrorResume(ExceptionUtils::isForbidden,
						err -> event.getUsername()
								.flatMap(username -> event.getChannel()
										.flatMap(channel -> ExceptionHandler.onForbidden(
												(ClientException) err, event.getGuildId().get(), channel, username)))
								.then())
				.subscribe();
	}

}
