package me.shadorc.shadbot.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Image.Format;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

//TODO: Use Discord4J's one
public class DiscordUtils {

	public static final int DESCRIPTION_CONTENT_LIMIT = 2048;
	public static final int FIELD_CONTENT_LIMIT = 1024;
	public static final int MAX_REASON_LENGTH = 512;

	public static String getChannelMention(Snowflake channelId) {
		return "<#" + channelId.asLong() + ">";
	}

	public static Mono<String> getAuthorAvatarUrl(Mono<User> author) {
		return author.map(user -> user.getAvatar(Format.JPEG))
				.map(image -> image.map(Image::getUrl))
				.map(url -> url.orElse("https://avatars0.githubusercontent.com/u/6373756?s=460&v=4"));

	}

	public static Instant getSnowflakeTimeFromID(Snowflake id) {
		return Instant.ofEpochMilli(1420070400000L + (id.asLong() >>> 22));
	}

	public static Mono<Boolean> hasPermissions(Mono<Member> member, Permission... permissions) {
		return member.flatMap(user -> DiscordUtils.hasPermissions(user, permissions));
	}

	public static Mono<Boolean> hasPermissions(Member member, Permission... permissions) {
		return member.getRoles()
				.map(Role::getPermissions)
				.map(ArrayList::new)
				.map(ArrayList::stream)
				.all(stream -> stream.allMatch(perm -> Arrays.asList(permissions).contains(perm)));
	}

	public static Mono<Boolean> hasPermissions(Mono<User> user, Snowflake guildId, Permission... permissions) {
		return user.flatMap(usr -> usr.asMember(guildId))
				.flatMap(member -> DiscordUtils.hasPermissions(member, permissions));
	}

	public static Mono<Optional<Snowflake>> getVoiceChannelId(Member member) {
		return member.getVoiceState()
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());
	}

	public static Mono<Optional<Snowflake>> getVoiceChannelId(Mono<Member> member) {
		return member.flatMap(DiscordUtils::getVoiceChannelId);
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
