package me.shadorc.shadbot.core.ratelimiter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.ConsumptionProbe;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.object.Emoji;

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
		this.bandwidth = Bandwidth.simple(max, duration);
	}

	public boolean isLimitedAndWarn(DiscordClient client, Snowflake guildId, Snowflake channelId, Snowflake userId) {
		guildsLimitedMap.putIfAbsent(guildId, new LimitedGuild(bandwidth));
		ConsumptionProbe probe = guildsLimitedMap.get(guildId)
				.getUserBucket(userId)
				.tryConsumeAndReturnRemaining(1);

		if(probe.getRemainingTokens() == 0) {
			this.sendWarningMessage(client, channelId, userId);
			return true;
		}

		if(probe.isConsumed()) {
			return true;
		}

		return false;
	}

	private void sendWarningMessage(DiscordClient client, Snowflake channelId, Snowflake userId) {
		client.getUserById(userId).subscribe(author -> {
			String username = author.getUsername();
			String message = TextUtils.getSpamMessage();
			String maxNum = StringUtils.pluralOf(max, "time");
			String durationStr = DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
			String text = String.format(Emoji.STOPWATCH + " (**%s**) %s You can use this command %s every *%s*.",
					username, message, maxNum, durationStr);

			BotUtils.sendMessage(text, client.getMessageChannelById(channelId));
		});
	}

}
