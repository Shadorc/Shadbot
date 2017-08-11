package me.shadorc.discordbot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	private final HashMap<IGuild, HashMap<IUser, Long>> guildsRateLimiter;
	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.guildsRateLimiter = new HashMap<>();
	}

	public boolean isLimited(IGuild guild, IUser user) {
		long currentTime = System.currentTimeMillis();

		if(!guildsRateLimiter.containsKey(guild)) {
			guildsRateLimiter.put(guild, new HashMap<IUser, Long>());
			return false;
		}

		if(!guildsRateLimiter.get(guild).containsKey(user)) {
			guildsRateLimiter.get(guild).put(user, currentTime);
			return false;
		}

		long lastTime = guildsRateLimiter.get(guild).get(user);
		long diff = currentTime - lastTime;
		if(diff > timeout) {
			guildsRateLimiter.get(guild).remove(user);
			return false;
		}

		return true;
	}

	public float getRemainingTime(IGuild guild, IUser user) {
		long remainingMillis = timeout - (System.currentTimeMillis() - guildsRateLimiter.get(guild).get(user));
		return remainingMillis / 1000f;
	}

	public long getTimeout() {
		return Duration.of(timeout, ChronoUnit.MILLIS).getSeconds();
	}
}