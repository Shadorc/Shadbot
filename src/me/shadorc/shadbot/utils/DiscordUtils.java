package me.shadorc.shadbot.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Image.Format;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiscordUtils {

	public static final int DESCRIPTION_CONTENT_LIMIT = 2048;
	public static final int FIELD_CONTENT_LIMIT = 1024;
	public static final int MAX_REASON_LENGTH = 512;

	public static Flux<Member> getMembers(Guild guild) {
		return DiscordUtils.getMembers(Mono.just(guild));
	}

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

	public static Mono<Long> getConnectedVoiceChannelCount(DiscordClient client) {
		return client.getGuilds()
				.flatMap(guild -> client.getSelf().flatMap(self -> self.asMember(guild.getId())))
				.flatMap(DiscordUtils::getVoiceChannelId)
				.flatMap(Mono::justOrEmpty)
				.count();
	}

	public static List<Snowflake> getChannelMentions(String content) {
		Matcher matcher = Pattern.compile("<#([0-9]{1,19})>").matcher(content);
		List<Snowflake> channelMentions = new ArrayList<>();
		while(matcher.find()) {
			channelMentions.add(Snowflake.of(matcher.group(1)));
		}
		return channelMentions.stream().distinct().collect(Collectors.toList());
	}

	public static String getChannelMention(Snowflake channelId) {
		return "<#" + channelId.asLong() + ">";
	}

	public static Mono<String> getAvatarUrl(Mono<User> user) {
		return user.map(DiscordUtils::getAvatarUrl);
	}

	public static String getAvatarUrl(User user) {
		return user.getAvatarUrl(Format.JPEG).orElse(user.getDefaultAvatarUrl());
	}

	public static Mono<Optional<Snowflake>> getVoiceChannelId(Mono<Member> member) {
		return member.flatMap(DiscordUtils::getVoiceChannelId);
	}

	public static Mono<Optional<Snowflake>> getVoiceChannelId(Member member) {
		return member.getVoiceState()
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());
	}

	public static Instant getSnowflakeTimeFromID(Snowflake id) {
		return Instant.ofEpochMilli(1420070400000L + (id.asLong() >>> 22));
	}

	public static Mono<Snowflake> requireSameVoiceChannel(Mono<Member> bot, Mono<Member> member) {
		return Mono.zip(DiscordUtils.getVoiceChannelId(bot),
				DiscordUtils.getVoiceChannelId(member),
				DiscordUtils.hasPermissions(bot, Permission.CONNECT, Permission.SPEAK))
				.map(tuple3 -> {
					final Optional<Snowflake> botVoiceChannelId = tuple3.getT1();
					final Optional<Snowflake> userVoiceChannelId = tuple3.getT2();
					final boolean hasPerm = tuple3.getT3();

					if(!botVoiceChannelId.isPresent() && !userVoiceChannelId.isPresent()) {
						throw new CommandException("Join a voice channel before using this command.");
					}

					if(!botVoiceChannelId.isPresent() && userVoiceChannelId.isPresent() && !hasPerm) {
						throw new MissingPermissionException();
					}

					if(botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.get()::equals).orElse(false)) {
						throw new CommandException(String.format("I'm currently playing music in voice channel %s"
								+ ", join me before using this command.", DiscordUtils.getChannelMention(botVoiceChannelId.get())));
					}

					return userVoiceChannelId.get();
				});
	}

	public static Mono<Boolean> hasPermissions(Mono<Member> member, Permission... permissions) {
		return member.flatMap(user -> DiscordUtils.hasPermissions(user, permissions));
	}

	public static Mono<Boolean> hasPermissions(Member member, Permission... permissions) {
		return member.getRoles()
				.map(Role::getPermissions)
				.flatMap(Flux::fromIterable)
				.collectList()
				.map(rolePermissions -> rolePermissions.containsAll(PermissionSet.of(permissions)));
	}

	public static int getRecommendedShardCount() {
		return 1;
	}

	public static <T extends Event> void registerListener(DiscordClient client, Class<T> eventClass, Consumer<? super T> consumer) {
		client.getEventDispatcher().on(eventClass)
				.doOnError(err -> LogUtils.error(client, err, String.format("An unknown error occurred on %s.", eventClass.getSimpleName())))
				.retry()
				.subscribe(consumer);
	}

}
