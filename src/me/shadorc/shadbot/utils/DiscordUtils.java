package me.shadorc.shadbot.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiscordUtils {

	/* Audit Log reason must be 512 or fewer in length. */
	public static final int MAX_REASON_LENGTH = 512;

	public static Mono<Void> updatePresence(DiscordClient client) {
		String text;
		if(ThreadLocalRandom.current().nextInt(2) == 0) {
			text = TextUtils.PLAYING.getText();
		} else {
			text = String.format("%shelp | %s", Config.DEFAULT_PREFIX, Utils.randValue(TextUtils.TIP_MESSAGES));
		}
		return client.updatePresence(Presence.online(Activity.playing(text)));

	}

	/**
	 * @param member - the member who bet
	 * @param betStr - the string representing the bet
	 * @param maxValue - the maximum bet value
	 * @return An Integer representing {@code betStr} converted as an integer
	 * @throws CommandException - thrown if {@code betStr} cannot be casted to integer, if the {@code user} does not have enough coins or if the bet value
	 *             is superior to {code maxValue}
	 */
	public static int requireBet(Member member, String betStr, int maxValue) {
		final Integer bet = NumberUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", betStr));
		}

		if(Shadbot.getDatabase().getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
			throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
		}

		if(bet > maxValue) {
			throw new CommandException(String.format("Sorry, you can't bet more than **%s**.",
					FormatUtils.coins(maxValue)));
		}

		return bet;
	}

	/**
	 * @param guild - a {@link Guild} {@link Mono} containing the roles to extract
	 * @param content - a string containing role mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted roles
	 */
	public static Flux<Snowflake> extractRoles(Mono<Guild> guild, String content) {
		final List<String> words = StringUtils.split(content);
		return guild.flatMapMany(Guild::getRoles)
				.filter(role -> words.contains(String.format("@%s", role.getName()))
						|| words.contains(role.getMention()))
				.map(Role::getId)
				.distinct();
	}

	/**
	 * @param guild - a {@link Guild} {@link Mono} containing the channels to extract
	 * @param content - a string containing channels mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted channels
	 */
	public static Flux<Snowflake> extractChannels(Mono<Guild> guild, String content) {
		final List<String> words = StringUtils.split(content);
		return guild.flatMapMany(Guild::getChannels)
				.filter(channel -> words.contains(String.format("#%s", channel.getName()))
						|| words.contains(channel.getMention()))
				.map(GuildChannel::getId)
				.distinct();
	}

	/**
	 * @param channel - the channel containing the messages to delete
	 * @param messages - the {@link List} of messages to delete
	 * @return The number of deleted messages
	 */
	public static Mono<Integer> bulkDelete(Mono<TextChannel> channel, List<Message> messages) {
		switch (messages.size()) {
			case 0:
				return Mono.just(messages.size());
			case 1:
				return messages.get(0)
						.delete()
						.thenReturn(messages.size());
			default:
				return channel
						.flatMap(channelItr -> channelItr.bulkDelete(Flux.fromIterable(messages)
								.map(Message::getId))
								.collectList()
								.map(messagesNotDeleted -> messages.size() - messagesNotDeleted.size()));
		}
	}

	/**
	 * @param message - the message
	 * @return The members mentioned in a {@link Message}
	 */
	public static Flux<Member> getMembersFrom(Message message) {
		return message.getGuild()
				.flatMapMany(Guild::getMembers)
				.filter(member -> message.mentionsEveryone()
						|| message.getUserMentionIds().contains(member.getId())
						|| !Collections.disjoint(member.getRoleIds(), message.getRoleMentionIds()));
	}

	public static String getChannelMention(Snowflake channelId) {
		return String.format("<#%d>", channelId.asLong());
	}

	public static Mono<Snowflake> requireSameVoiceChannel(Context context) {
		final Mono<Optional<Snowflake>> botVoiceChannelIdMono = context.getSelfAsMember()
				.flatMap(Member::getVoiceState)
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());

		final Mono<Optional<Snowflake>> userVoiceChannelIdMono = context.getMember()
				.getVoiceState()
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());

		return Mono.zip(botVoiceChannelIdMono, userVoiceChannelIdMono)
				.map(tuple -> {
					final Optional<Snowflake> botVoiceChannelId = tuple.getT1();
					final Optional<Snowflake> userVoiceChannelId = tuple.getT2();

					// If the user is in a voice channel but the bot is not allowed to join
					if(userVoiceChannelId.isPresent() && !BotUtils.isVoiceChannelAllowed(context.getGuildId(), userVoiceChannelId.get())) {
						throw new CommandException("I'm not allowed to join this voice channel.");
					}

					// If the user and the bot are not in a voice channel
					if(!botVoiceChannelId.isPresent() && !userVoiceChannelId.isPresent()) {
						throw new CommandException("Join a voice channel before using this command.");
					}

					// If the user and the bot are not in the same voice channel
					if(botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.get()::equals).orElse(false)) {
						throw new CommandException(String.format("I'm currently playing music in voice channel <#%d>"
								+ ", join me before using this command.", botVoiceChannelId.map(Snowflake::asLong).get()));
					}

					return userVoiceChannelId.get();
				});
	}

	public static Mono<Boolean> hasPermission(Mono<MessageChannel> channel, Snowflake memberId, Permission permission) {
		return channel
				.ofType(GuildChannel.class)
				.flatMap(guildChannel -> guildChannel.getEffectivePermissions(memberId))
				.map(permissions -> permissions.contains(permission));
	}

	public static Mono<Void> requirePermissions(Mono<MessageChannel> channel, Snowflake userId, UserType userType, Permission... permissions) {
		return channel
				.ofType(GuildChannel.class)
				.flatMap(guildChannel -> guildChannel.getEffectivePermissions(userId))
				.doOnSuccess(effectivePermissions -> {
					for(Permission permission : permissions) {
						if(!effectivePermissions.contains(permission)) {
							throw new MissingPermissionException(userType, permission);
						}
					}
				})
				.then();
	}

}
