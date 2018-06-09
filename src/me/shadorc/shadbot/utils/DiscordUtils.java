package me.shadorc.shadbot.utils;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

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

	public static Instant getSnowflakeTimeFromID(Snowflake id) {
		return Instant.ofEpochMilli(1420070400000L + (id.asLong() >>> 22));
	}

	public static int convertColor(Color color) {
		return ((color.getRed() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getBlue() & 0xFF);
	}

	public static Mono<Boolean> hasPermissions(Mono<Member> member, Permission... permissions) {
		return member.flatMapMany(Member::getRoles)
				.map(Role::getPermissions)
				.map(ArrayList::new)
				.map(ArrayList::stream)
				.all(stream -> stream.allMatch(perm -> Arrays.asList(permissions).contains(perm)));
	}

	public static Mono<Boolean> hasPermissions(Mono<User> member, Snowflake guildId, Permission... permissions) {
		return DiscordUtils.hasPermissions(member.flatMap(user -> user.asMember(guildId)), permissions);
	}

}
