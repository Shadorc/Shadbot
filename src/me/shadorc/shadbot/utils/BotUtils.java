package me.shadorc.shadbot.utils;

import java.util.Collections;
import java.util.List;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotUtils {

	public static Mono<Message> sendMessage(String content, Mono<MessageChannel> channel) {
		return channel.flatMap(chnl -> BotUtils.sendMessage(content, chnl));
	}

	public static Mono<Message> sendMessage(String content, MessageChannel channel) {
		return BotUtils.sendMessage(new MessageCreateSpec().setContent(content), channel);
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, Mono<MessageChannel> channel) {
		return channel.flatMap(chnl -> BotUtils.sendMessage(embed, chnl));
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, MessageChannel channel) {
		return BotUtils.sendMessage(new MessageCreateSpec().setEmbed(embed), channel)
				.doOnSuccess(msg -> VariousStatsManager.log(VariousEnum.EMBEDS_SENT))
				.doOnError(ExceptionUtils::isForbidden,
						err -> {
							BotUtils.sendMessage(TextUtils.missingPerm(Permission.EMBED_LINKS), channel);
							LogUtils.infof("{Channel ID: %d} Shadbot was not allowed to send an embed.", channel.getId().asLong());
						});
	}

	public static Mono<Message> sendMessage(String content, EmbedCreateSpec embed, Mono<MessageChannel> channel) {
		return channel.flatMap(chnl -> BotUtils.sendMessage(new MessageCreateSpec().setContent(content).setEmbed(embed), chnl));
	}

	private static Mono<Message> sendMessage(MessageCreateSpec message, MessageChannel channel) {
		return channel.createMessage(message)
				.doOnSuccess(msg -> VariousStatsManager.log(VariousEnum.MESSAGES_SENT))
				.doOnError(ExceptionUtils::isForbidden,
						err -> LogUtils.infof("{Channel ID: %d} Shadbot was not allowed to send a message.", channel.getId().asLong()))
				.doOnError(ExceptionUtils::isNotForbidden,
						err -> LogUtils.error(channel.getClient(), err,
								String.format("{Channel ID: %d} An error occurred while sending a message.", channel.getId().asLong())));
	}

	// TOOD
	/**
	 * @param channel
	 * @param messages
	 * @return The number of deleted messages
	 */
	public static Mono<Long> deleteMessages(TextChannel channel, List<Message> messages) {
		switch (messages.size()) {
			case 0:
				return Mono.just(0L);
			case 1:
				return messages.get(0).delete().flatMap(message -> {
					return Mono.just(1L);
				});
			default:
				return channel.bulkDelete(Flux.fromIterable(messages).map(Message::getId)).buffer().single().flatMap(list -> {
					return Mono.just((long) (messages.size() - list.size()));
				});
		}
	}

	public static void updatePresence(DiscordClient client) {
		String text = String.format("%shelp | %s", Config.DEFAULT_PREFIX, TextUtils.getTip());
		client.updatePresence(Presence.online(Activity.playing(text))).subscribe();
	}

	public static Flux<User> getUsersFrom(Message message) {
		return message.getUserMentions()
				.concatWith(message.getGuild().flatMapMany(Guild::getMembers)
						.filter(member -> !Collections.disjoint(member.getRoleIds(), message.getRoleMentionIds()))
						.map(User.class::cast))
				.distinct();
	}

	public static boolean hasAllowedRole(Snowflake guildId, List<Role> roles) {
		List<Snowflake> allowedRoles = DatabaseManager.getDBGuild(guildId).getAllowedRoles();
		// If the user is an administrator OR no permissions have been set OR the role is allowed
		return roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
				|| allowedRoles.isEmpty()
				|| roles.stream().anyMatch(role -> allowedRoles.contains(role.getId()));
	}

	public static boolean isChannelAllowed(Snowflake guildId, Snowflake channelId) {
		List<Snowflake> allowedChannels = DatabaseManager.getDBGuild(guildId).getAllowedChannels();
		// If no permission has been set OR the channel is allowed
		return allowedChannels.isEmpty() || allowedChannels.contains(channelId);
	}

	public static boolean isCommandAllowed(Snowflake guildId, AbstractCommand cmd) {
		List<String> blacklistedCmd = DatabaseManager.getDBGuild(guildId).getBlacklistedCmd();
		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

}
