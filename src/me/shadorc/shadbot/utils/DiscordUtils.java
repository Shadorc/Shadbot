package me.shadorc.shadbot.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import discord4j.core.DiscordClient;
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
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiscordUtils {

	/* Audit Log reason must be 512 or fewer in length. */
	public static final int MAX_REASON_LENGTH = 512;

	public static Mono<Void> updatePresence(DiscordClient client) {
		return Mono.just(String.format("%shelp | %s", Config.DEFAULT_PREFIX, Utils.randValue(TextUtils.TIP_MESSAGES)))
				.flatMap(text -> client.updatePresence(Presence.online(Activity.playing(text))));
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

		if(DatabaseManager.getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
			throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
		}

		if(bet > maxValue) {
			throw new CommandException(String.format("Sorry, you can't bet more than **%s**.",
					FormatUtils.coins(maxValue)));
		}

		return bet;
	}

	/**
	 * @param client - the client from which to post statistics
	 */
	public static Mono<Void> postStats(DiscordClient client) {
		if(Config.IS_SNAPSHOT) {
			return Mono.empty();
		}
		return Mono.fromRunnable(() -> LogUtils.info("{Shard %d} Posting statistics...", client.getConfig().getShardIndex()))
				.then(DiscordUtils.postStatsOn(client, "https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN))
				.then(DiscordUtils.postStatsOn(client, "https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN))
				.then(Mono.fromRunnable(() -> LogUtils.info("{Shard %d} Statistics posted.", client.getConfig().getShardIndex())));
	}

	/**
	 * @param homeUrl - the statistics site URL
	 * @param token - the API token corresponding to the website
	 * @param client - the client from which to post statistics
	 */
	private static Mono<Void> postStatsOn(DiscordClient client, String homeUrl, APIKey token) {
		return client.getGuilds().count()
				.doOnSuccess(guildsCount -> {
					final JSONObject content = new JSONObject()
							.put("shard_id", client.getConfig().getShardIndex())
							.put("shard_count", client.getConfig().getShardCount())
							.put("server_count", guildsCount);
					final String url = String.format("%s/api/bots/%d/stats", homeUrl, client.getSelfId().get().asLong());

					try {
						Jsoup.connect(url)
								.method(Method.POST)
								.ignoreContentType(true)
								.headers(Map.of("Content-Type", "application/json", "Authorization", APIKeys.get(token)))
								.requestBody(content.toString())
								.post();
					} catch (IOException err) {
						Exceptions.propagate(err);
					}
				})
				.then();
	}

	public static Flux<Snowflake> extractRoles(Message message) {
		final List<String> words = StringUtils.split(message.getContent().orElse("")).stream()
				.map(word -> word.replace("@", ""))
				.collect(Collectors.toList());

		return message.getGuild()
				.flatMapMany(Guild::getRoles)
				.filter(role -> words.contains(role.getName()))
				.map(Role::getId)
				.concatWith(Flux.fromIterable(message.getRoleMentionIds()));
	}

	public static Flux<Snowflake> extractChannels(Message message) {
		final String content = message.getContent().orElse("");
		final List<String> words = StringUtils.split(content).stream()
				.map(String::toLowerCase)
				.map(word -> word.replace("#", ""))
				.collect(Collectors.toList());

		return message.getGuild()
				.flatMapMany(Guild::getChannels)
				.filter(channel -> words.contains(channel.getName()))
				.map(GuildChannel::getId)
				.concatWith(Flux.fromIterable(DiscordUtils.extractChannelMentions(content)));
	}

	private static List<Snowflake> extractChannelMentions(String content) {
		final Matcher matcher = Pattern.compile("<#([0-9]{1,19})>").matcher(content);
		final List<Snowflake> channelMentions = new ArrayList<>();
		while(matcher.find()) {
			channelMentions.add(Snowflake.of(matcher.group(1)));
		}
		return channelMentions.stream().distinct().collect(Collectors.toList());
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
		return DiscordUtils.getMembers(message.getGuild())
				.filter(member -> message.mentionsEveryone()
						|| message.getUserMentionIds().contains(member.getId())
						|| !Collections.disjoint(member.getRoleIds(), message.getRoleMentionIds()));
	}

	// Fix: https://github.com/Discord4J/Discord4J/issues/429
	public static Flux<Member> getMembers(Guild guild) {
		return DiscordUtils.getMembers(Mono.just(guild));
	}

	// Fix: https://github.com/Discord4J/Discord4J/issues/429
	public static Flux<Member> getMembers(Mono<Guild> guild) {
		return guild.flatMapMany(Guild::getMembers)
				.collectList()
				.map(list -> {
					final List<Member> members = new ArrayList<>();
					for(Member member : list) {
						if(!members.stream().map(Member::getId).anyMatch(member.getId()::equals)) {
							members.add(member);
						}
					}
					return members;
				})
				.flatMapMany(Flux::fromIterable);
	}

	public static String getChannelMention(Snowflake channelId) {
		return String.format("<#%d>", channelId.asLong());
	}

	// TODO: Implement
	// public static Mono<Snowflake> requireSameVoiceChannel(Context context) {
	// return Mono.zip(DiscordUtils.getVoiceChannelId(context.getSelfAsMember()),
	// DiscordUtils.getVoiceChannelId(context.getMessage().getAuthorAsMember()))
	// .map(tuple2 -> {
	// final Optional<Snowflake> botVoiceChannelId = tuple2.getT1();
	// final Optional<Snowflake> userVoiceChannelId = tuple2.getT2();
	//
	// if(userVoiceChannelId.isPresent() && !BotUtils.isVoiceChannelAllowed(context.getGuildId(), userVoiceChannelId.get())) {
	// throw new CommandException("I'm not allowed to join this voice channel.");
	// }
	//
	// if(!botVoiceChannelId.isPresent() && !userVoiceChannelId.isPresent()) {
	// throw new CommandException("Join a voice channel before using this command.");
	// }
	//
	// if(botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.get()::equals).orElse(false)) {
	// throw new CommandException(String.format("I'm currently playing music in voice channel %s"
	// + ", join me before using this command.", DiscordUtils.mentionChannel(botVoiceChannelId.get())));
	// }
	//
	// return userVoiceChannelId.get();
	// });
	// }

	public static Mono<Boolean> hasPermission(Mono<MessageChannel> channel, Snowflake memberId, Permission permission) {
		return channel
				.ofType(TextChannel.class)
				.flatMap(textChannel -> textChannel.getEffectivePermissions(memberId))
				.map(permissions -> permissions.contains(permission));
	}

	public static Mono<Void> requirePermissions(Mono<MessageChannel> channel, Snowflake userId, UserType userType, Permission... permissions) {
		return channel
				.ofType(TextChannel.class)
				.flatMap(textChannel -> textChannel.getEffectivePermissions(userId))
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
