package me.shadorc.discordbot;

import java.util.concurrent.ConcurrentHashMap;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	private static final ConcurrentHashMap <IGuild, ConcurrentHashMap <IUser, Long>> GUILDS_RATELIMIT = new ConcurrentHashMap<>();

	private int timeout;

	public RateLimiter(int timeout) {
		this.timeout = timeout;
	}

	public boolean isRateLimited(IGuild guild, IUser user) {
		long currentTime = System.currentTimeMillis();

		if(!GUILDS_RATELIMIT.containsKey(guild)) {
			GUILDS_RATELIMIT.put(guild, new ConcurrentHashMap <IUser, Long>());
			GUILDS_RATELIMIT.get(guild).put(user, currentTime);
			return false;
		}

		long lastTime = GUILDS_RATELIMIT.get(guild).get(user);
		long diff = currentTime - lastTime;
		if(diff > timeout*1000) {
			GUILDS_RATELIMIT.get(guild).put(user, currentTime);
			return false;
		}

		return true;
	}

	public long getRemainingTime(IGuild guild, IUser user) {
		return timeout - (System.currentTimeMillis() - GUILDS_RATELIMIT.get(guild).get(user))/1000;
	}

	public int getTimeout() {
		return timeout;
	}
}