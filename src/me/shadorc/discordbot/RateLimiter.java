package me.shadorc.discordbot;

import java.util.HashMap;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	private static final HashMap<IGuild, HashMap<IUser, Long>> GUILDS_RATELIMITER = new HashMap<>();

	private final int timeout;

	public RateLimiter(int timeout) {
		this.timeout = timeout;
	}

	public boolean isRateLimited(IGuild guild, IUser user) {
		long currentTime = System.currentTimeMillis();

		if(!GUILDS_RATELIMITER.containsKey(guild)) {
			GUILDS_RATELIMITER.put(guild, new HashMap<IUser, Long>());
			GUILDS_RATELIMITER.get(guild).put(user, currentTime);
			return false;
		}

		long lastTime = GUILDS_RATELIMITER.get(guild).get(user);
		long diff = currentTime - lastTime;
		if(diff > timeout * 1000) {
			GUILDS_RATELIMITER.get(guild).put(user, currentTime);
			return false;
		}

		return true;
	}

	public long getRemainingTime(IGuild guild, IUser user) {
		return timeout - (System.currentTimeMillis() - GUILDS_RATELIMITER.get(guild).get(user)) / 1000;
	}

	public int getTimeout() {
		return timeout;
	}
}