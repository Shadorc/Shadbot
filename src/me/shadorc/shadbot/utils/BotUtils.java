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
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotUtils {

	public static void sendMessage(String content, Mono<MessageChannel> channel) {
		channel.subscribe(msgChannel -> BotUtils.sendMessage(content, msgChannel));
	}

	public static void sendMessage(String content, MessageChannel channel) {
		BotUtils.sendMessage(new MessageCreateSpec().setContent(content), channel).subscribe();
	}

	public static void sendMessage(EmbedCreateSpec embed, Mono<MessageChannel> channel) {
		channel.subscribe(msgChannel -> BotUtils.sendMessage(embed, msgChannel));
	}

	public static void sendMessage(EmbedCreateSpec embed, MessageChannel channel) {
		VariousStatsManager.log(VariousEnum.EMBEDS_SENT);

		BotUtils.sendMessage(new MessageCreateSpec().setEmbed(embed), channel)
				.doOnError(ExceptionUtils::isForbidden,
						err -> BotUtils.sendMessage(TextUtils.missingPerm(Permission.EMBED_LINKS), channel))
				.subscribe();
	}

	public static void sendMessage(MessageCreateSpec message, Mono<MessageChannel> channel) {
		channel.subscribe(msgChannel -> BotUtils.sendMessage(message, msgChannel).subscribe());
	}

	public static Mono<Message> sendMessage(MessageCreateSpec message, MessageChannel channel) {
		VariousStatsManager.log(VariousEnum.MESSAGES_SENT);

		return channel.createMessage(message)
				.doOnError(ExceptionUtils::isForbidden,
						err -> LogUtils.infof("{Channel ID: %s} Shadbot was not allowed to send a message.", channel.getId()))
				.doOnError(
						err -> {
							LogUtils.error(err, String.format("{Channel ID: %s} An error occurred while sending a message.", channel.getId()));
						});
	}

	// TODO: This need to be subscribed
	public static Flux<Snowflake> deleteMessages(TextChannel channel, Message... messages) {
		switch (messages.length) {
			case 0:
				return Flux.empty();
			case 1:
				messages[0].delete();
				return Flux.just(messages[0].getId());
			default:
				return channel.bulkDelete(Flux.fromArray(messages).map(Message::getId));
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
		List<Snowflake> allowedRoles = Database.getDBGuild(guildId).getAllowedRoles();
		// If the user is an administrator OR no permissions have been set OR the role is allowed
		return roles.stream().anyMatch(role -> role.getPermissions().contains(Permission.ADMINISTRATOR))
				|| allowedRoles.isEmpty()
				|| roles.stream().anyMatch(role -> allowedRoles.contains(role.getId()));
	}

	public static boolean isChannelAllowed(Snowflake guildId, Snowflake channelId) {
		List<Snowflake> allowedChannels = Database.getDBGuild(guildId).getAllowedChannels();
		// If no permission has been set OR the channel is allowed
		return allowedChannels.isEmpty() || allowedChannels.contains(channelId);
	}

	public static boolean isCommandAllowed(Snowflake guildId, AbstractCommand cmd) {
		List<String> blacklistedCmd = Database.getDBGuild(guildId).getBlacklistedCmd();
		return cmd.getNames().stream().noneMatch(blacklistedCmd::contains);
	}

}
