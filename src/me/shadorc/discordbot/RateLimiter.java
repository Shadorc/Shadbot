package me.shadorc.discordbot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	private static final HashMap<IGuild, HashMap<IUser, Long>> GUILDS_RATELIMITER = new HashMap<>();

	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
	}

	public boolean isLimited(IGuild guild, IUser user) {
		long currentTime = System.currentTimeMillis();

		if(!GUILDS_RATELIMITER.containsKey(guild)) {
			GUILDS_RATELIMITER.put(guild, new HashMap<IUser, Long>());
			GUILDS_RATELIMITER.get(guild).put(user, currentTime);
			return false;
		}

		long lastTime = GUILDS_RATELIMITER.get(guild).get(user);
		long diff = currentTime - lastTime;
		if(diff > timeout) {
			GUILDS_RATELIMITER.get(guild).put(user, currentTime);
			return false;
		}

		return true;
	}

	public long getRemainingTime(IGuild guild, IUser user) {
		return timeout - (System.currentTimeMillis() - GUILDS_RATELIMITER.get(guild).get(user));
	}

	public long getTimeout() {
		return Duration.of(timeout, ChronoUnit.MILLIS).getSeconds();
	}
}