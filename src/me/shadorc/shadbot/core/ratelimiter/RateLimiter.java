package me.shadorc.shadbot.core.ratelimiter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.TemporaryMessage;
import reactor.core.publisher.Mono;

public class RateLimiter {

	private final ConcurrentHashMap<Snowflake, LimitedGuild> guildsLimitedMap;
	private final int max;
	private final Duration duration;
	private final Bandwidth bandwidth;

	public RateLimiter(int max, Duration duration) {
		this.guildsLimitedMap = new ConcurrentHashMap<>();
		this.max = max;
		this.duration = duration;
		this.bandwidth = Bandwidth.classic(this.max, Refill.intervally(this.max, this.duration));
	}

	public boolean isLimitedAndWarn(DiscordClient client, Snowflake guildId, Snowflake channelId, Snowflake userId) {
		this.guildsLimitedMap.putIfAbsent(guildId, new LimitedGuild(this.bandwidth));

		final LimitedUser limitedUser = this.guildsLimitedMap.get(guildId)
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
					final String message = TextUtils.SPAMS.getText();
					final String maxNum = StringUtils.pluralOf(this.max, "time");
					final String durationStr = DurationFormatUtils.formatDurationWords(this.duration.toMillis(), true, true);
					return String.format(Emoji.STOPWATCH + " (**%s**) %s You can use this command %s every *%s*.",
							username, message, maxNum, durationStr);
				})
				.flatMap(new TemporaryMessage(client, channelId, 10, ChronoUnit.SECONDS)::send)
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(client, err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(client, err));
	}

}
