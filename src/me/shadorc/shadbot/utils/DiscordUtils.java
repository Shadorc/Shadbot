package me.shadorc.shadbot.utils;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

//TODO: Use Discord4J's one
public class DiscordUtils {

	public static final int DESCRIPTION_CONTENT_LIMIT = 2048;
	public static final int FIELD_CONTENT_LIMIT = 1024;
	public static final int MAX_REASON_LENGTH = 512;

	private static final String AVATARS = "https://cdn.discordapp.com/avatars/%s/%s.%s";
	public static final String DEFAULT_AVATAR = "https://cdn.discordapp.com/embed/avatars/%d.png";

	public static String getAvatarUrl(User user) {
		if(user.getAvatarHash().isPresent()) {
			return String.format(AVATARS, user.getId().asLong(),
					user.getAvatarHash().get(),
					user.getAvatarHash().get().startsWith("a_") ? "gif" : "webp");
		} else {
			return String.format(DEFAULT_AVATAR,
					Integer.parseInt(user.getDiscriminator()) % 5);
		}
	}

	public static Mono<String> getAvatarUrl(Mono<User> monoUser) {
		return monoUser.map(DiscordUtils::getAvatarUrl);
	}

	public static Instant getSnowflakeTimeFromID(Snowflake id) {
		return Instant.ofEpochMilli(1420070400000L + (id.asLong() >>> 22));
	}

	public static int convertColor(Color color) {
		return ((color.getRed() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getBlue() & 0xFF);
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

	public static Mono<Optional<Snowflake>> getVoiceChannelId(Mono<Member> member) {
		return member.flatMap(Member::getVoiceState)
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());
	}

}
