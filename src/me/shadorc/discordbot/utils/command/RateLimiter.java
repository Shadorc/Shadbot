package me.shadorc.discordbot.utils.command;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Stats.Category;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	public static final int COMMON_COOLDOWN = 2;
	public static final int GAME_COOLDOWN = 5;

	private final ConcurrentHashMap<IGuild, ConcurrentHashMap<IUser, Long>> guildsRateLimiter;
	private final ConcurrentHashMap<IGuild, ConcurrentHashMap<IUser, Boolean>> warningsRateLimiter;
	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.guildsRateLimiter = new ConcurrentHashMap<>();
		this.warningsRateLimiter = new ConcurrentHashMap<>();
	}

	public long getTimeout() {
		return Duration.of(timeout, ChronoUnit.MILLIS).getSeconds();
	}

	public boolean isSpamming(Context context) {
		if(this.isLimited(context.getGuild(), context.getAuthor())) {
			if(!this.isWarned(context.getGuild(), context.getAuthor())) {
				this.warn("Take it easy, don't spam :) You can use this command once every " + this.getTimeout() + " sec.", context);
			}
			return true;
		}
		return false;
	}

	private boolean isLimited(IGuild guild, IUser user) {
		guildsRateLimiter.putIfAbsent(guild, new ConcurrentHashMap<IUser, Long>());
		warningsRateLimiter.putIfAbsent(guild, new ConcurrentHashMap<IUser, Boolean>());

		long currentTime = System.currentTimeMillis();
		long lastTime = guildsRateLimiter.get(guild).containsKey(user) ? guildsRateLimiter.get(guild).get(user) : 0;
		long diff = currentTime - lastTime;

		if(diff > timeout) {
			guildsRateLimiter.get(guild).put(user, currentTime);
			warningsRateLimiter.get(guild).put(user, false);
			return false;
		}

		return true;
	}

	private boolean isWarned(IGuild guild, IUser user) {
		return warningsRateLimiter.get(guild).get(user);
	}

	private void warn(String message, Context context) {
		BotUtils.send(Emoji.STOPWATCH + " " + message, context.getChannel());
		warningsRateLimiter.get(context.getGuild()).put(context.getAuthor(), true);
		Stats.increment(Category.LIMITED_COMMAND, context.getCommand());
	}
}