package me.shadorc.discordbot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	private final Map<IGuild, HashMap<IUser, Long>> guildsRateLimiter;
	private final Map<IGuild, HashMap<IUser, Boolean>> warningsRateLimiter;
	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.guildsRateLimiter = new HashMap<>();
		this.warningsRateLimiter = new HashMap<>();
	}

	public boolean isLimited(IGuild guild, IUser user) {
		if(!guildsRateLimiter.containsKey(guild)) {
			guildsRateLimiter.put(guild, new HashMap<IUser, Long>());
			warningsRateLimiter.put(guild, new HashMap<IUser, Boolean>());
		}

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

	public boolean isWarned(IGuild guild, IUser user) {
		return warningsRateLimiter.get(guild).get(user);
	}

	public float getRemainingTime(IGuild guild, IUser user) {
		long remainingMillis = timeout - (System.currentTimeMillis() - guildsRateLimiter.get(guild).get(user));
		return remainingMillis / 1000f;
	}

	public long getTimeout() {
		return Duration.of(timeout, ChronoUnit.MILLIS).getSeconds();
	}

	public void warn(String message, Context context) {
		LogUtils.info("{RateLimiter} {Guild: " + context.getGuild().getName() + " (ID: " + context.getGuild().getStringID() + ")} "
				+ "User (ID:" + context.getAuthor().getStringID() + ") warned. Command: " + context.getCommand());
		BotUtils.sendMessage(Emoji.STOPWATCH + " " + message, context.getChannel());
		warningsRateLimiter.get(context.getGuild()).put(context.getAuthor(), true);
	}
}