package me.shadorc.shadbot.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.util.Snowflake;

public class LimitedGuild {

	private final ConcurrentHashMap<Snowflake, LimitedUser> limitedUsersMap;

	public LimitedGuild() {
		this.limitedUsersMap = new ConcurrentHashMap<>();
	}

	public LimitedUser getUser(Snowflake userId) {
		return limitedUsersMap.get(userId);
	}

	public void addUserIfAbsent(Snowflake userId) {
		limitedUsersMap.putIfAbsent(userId, new LimitedUser());
	}

	public void scheduledDeletion(ScheduledThreadPoolExecutor scheduledExecutor, Snowflake userId, int cooldown) {
		ScheduledFuture<LimitedUser> deletionTask = limitedUsersMap.get(userId).getDeletionTask();
		if(deletionTask != null) {
			deletionTask.cancel(false);
		}

		deletionTask = scheduledExecutor.schedule(() -> limitedUsersMap.remove(userId), cooldown, TimeUnit.MILLISECONDS);
		limitedUsersMap.get(userId).setDeletionTask(deletionTask);
	}

}
