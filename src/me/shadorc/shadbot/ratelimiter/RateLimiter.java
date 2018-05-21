package me.shadorc.shadbot.ratelimiter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;
import me.shadorc.shadbot.utils.object.Emoji;

public class RateLimiter {

	public static final int DEFAULT_COOLDOWN = 5;
	public static final int GAME_COOLDOWN = 5;

	private final ScheduledThreadPoolExecutor scheduledExecutor;
	private final ConcurrentHashMap<Snowflake, LimitedGuild> guildsLimitedMap;
	private final int max;
	private final int cooldown;

	public RateLimiter(int max, int cooldown, ChronoUnit unit) {
		this.scheduledExecutor = new ScheduledWrappedExecutor("RateLimiter-%d");
		this.guildsLimitedMap = new ConcurrentHashMap<>();
		this.max = max;
		this.cooldown = (int) Duration.of(cooldown, unit).toMillis();
	}

	public boolean isLimited(Snowflake guildId, Snowflake channelId, Snowflake userId) {
		guildsLimitedMap.putIfAbsent(guildId, new LimitedGuild());

		LimitedGuild limitedGuild = guildsLimitedMap.get(guildId);
		limitedGuild.addUserIfAbsent(userId);

		LimitedUser limitedUser = limitedGuild.getUser(userId);
		limitedUser.increment();

		// The user has not exceeded the limit yet, he is not limited
		if(limitedUser.getCount() <= max) {
			limitedGuild.scheduledDeletion(scheduledExecutor, userId, cooldown);
			return false;
		}

		// The user has exceeded the limit, he's warned and limited
		if(limitedUser.getCount() == max + 1) {
			limitedGuild.scheduledDeletion(scheduledExecutor, userId, cooldown);
			this.warn(channelId, userId);
			return true;
		}

		// The user has already exceeded the limit, he will be unlimited when the deletion task will be done
		return true;
	}

	private void warn(Snowflake channelId, Snowflake userId) {
		Shadbot.getClient().getMessageChannelById(channelId).subscribe(channel -> {
			Shadbot.getClient().getUserById(userId).subscribe(user -> {
				BotUtils.sendMessage(String.format(Emoji.STOPWATCH + " (**%s**) %s You can use this command %s every *%s*.",
						user.getUsername(),
						TextUtils.getSpamMessage(),
						StringUtils.pluralOf(max, "time"),
						DurationFormatUtils.formatDurationWords(cooldown, true, true)), channel);
			});
		});
	}

}
