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
import me.shadorc.shadbot.command.admin.IamCmd;
import me.shadorc.shadbot.data.database.DatabaseManager;
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
			return reactionAddEvent == null ? reactionRemoveEvent.getClient() : reactionAddEvent.getClient();
		}

		private Mono<Message> getMessage() {
			return reactionAddEvent == null ? reactionRemoveEvent.getMessage() : reactionAddEvent.getMessage();
		}

		private Mono<MessageChannel> getChannel() {
			return reactionAddEvent == null ? reactionRemoveEvent.getChannel() : reactionAddEvent.getChannel();
		}

		private Optional<Snowflake> getSelfId() {
			return this.getClient().getSelfId();
		}

		private Mono<Member> getMember() {
			return this.getClient().getMemberById(this.getChannelId(), this.getUserId());
		}

		private Snowflake getChannelId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getChannelId() : reactionAddEvent.getChannelId();
		}

		private Optional<Snowflake> getGuildId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getGuildId() : reactionAddEvent.getGuildId();
		}

		private Snowflake getUserId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getUserId() : reactionAddEvent.getUserId();
		}

		private Snowflake getMessageId() {
			return reactionAddEvent == null ? reactionRemoveEvent.getMessageId() : reactionAddEvent.getMessageId();
		}

		private ReactionEmoji getEmoji() {
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
		Mono.justOrEmpty(event.getSelfId())
				// If the bot is not the author of the message, this is not an Iam message
				.filterWhen(selfId -> event.getMessage()
						.map(Message::getAuthorId)
						.flatMap(Mono::justOrEmpty)
						.map(selfId::equals))
				.flatMap(selfId -> Mono.justOrEmpty(event.getGuildId()))
				.flatMap(guildId -> Mono.justOrEmpty(DatabaseManager.getDBGuild(guildId)
						.getIamMessages()
						.get(event.getMessageId().asString())))
				.map(Snowflake::of)
				.filter(roleId -> event.getEmoji().equals(IamCmd.REACTION))
				.filterWhen(roleId -> DiscordUtils.hasPermission(event.getChannel(), event.getUserId(), Permission.MANAGE_ROLES)
						.flatMap(hasPerm -> {
							if(!hasPerm) {
								return new TemporaryMessage(event.getClient(), event.getChannelId(), 10, ChronoUnit.SECONDS)
										.send(String.format(Emoji.ACCESS_DENIED
												+ " I can't add/remove a role due to the lack of permission."
												+ "%nPlease, check my permissions to verify that %s is checked.",
												String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
										.thenReturn(hasPerm);
							}
							return Mono.just(hasPerm);
						}))
				.flatMap(roleId -> event.getMember()
						.flatMap(member -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId)))
				.subscribe();
	}

}
