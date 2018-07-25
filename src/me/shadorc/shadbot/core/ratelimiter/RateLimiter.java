package me.shadorc.shadbot.core.ratelimiter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.message.TemporaryMessage;

public class RateLimiter {

	public static final int DEFAULT_COOLDOWN = 5;
	public static final int GAME_COOLDOWN = 5;

	private final ConcurrentHashMap<Snowflake, LimitedGuild> guildsLimitedMap;
	private final int max;
	private final Duration duration;
	private final Bandwidth bandwidth;

	public RateLimiter(int max, int cooldown, ChronoUnit unit) {
		this.guildsLimitedMap = new ConcurrentHashMap<>();
		this.max = max;
		this.duration = Duration.of(cooldown, unit);
		this.bandwidth = Bandwidth.classic(max, Refill.intervally(max, duration));
	}

	public boolean isLimitedAndWarn(DiscordClient client, Snowflake guildId, Snowflake channelId, Snowflake userId) {
		guildsLimitedMap.putIfAbsent(guildId, new LimitedGuild(bandwidth));

		final LimitedUser limitedUser = guildsLimitedMap.get(guildId)
				.getUser(userId);

		// The token could not been consumed, the user is limited
		if(!limitedUser.getBucket().tryConsume(1)) {
			// The user has not yet been warned
			if(!limitedUser.isWarned()) {
				this.sendWarningMessage(client, channelId, userId);
				limitedUser.warn();
			}
			return true;
		}

		limitedUser.unwarn();
		return false;
	}

	private void sendWarningMessage(DiscordClient client, Snowflake channelId, Snowflake userId) {
		client.getUserById(userId)
				.map(author -> {
					final String username = author.getUsername();
					final String message = Utils.randValue(TextUtils.SPAM_MESSAGES);
					final String maxNum = StringUtils.pluralOf(max, "time");
					final String durationStr = DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
					return String.format(Emoji.STOPWATCH + " (**%s**) %s You can use this command %s every *%s*.",
							username, message, maxNum, durationStr);
				})
				.flatMap(text -> new TemporaryMessage(client, channelId, 10, TimeUnit.SECONDS).send(text))
				.subscribe();
	}

}
