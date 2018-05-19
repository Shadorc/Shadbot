package me.shadorc.shadbot.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;

public class LimitedGuild {

	private final ConcurrentHashMap<Snowflake, LimitedUser> limitedUsersMap;

	public LimitedGuild() {
		this.limitedUsersMap = new ConcurrentHashMap<>();
	}

	public LimitedUser getUser(User user) {
		return limitedUsersMap.get(user.getId());
	}

	public void addUserIfAbsent(User user) {
		limitedUsersMap.putIfAbsent(user.getId(), new LimitedUser());
	}

	public void scheduledDeletion(ScheduledThreadPoolExecutor scheduledExecutor, User user, int cooldown) {
		ScheduledFuture<LimitedUser> deletionTask = limitedUsersMap.get(user.getId()).getDeletionTask();
		if(deletionTask != null) {
			deletionTask.cancel(false);
		}

		deletionTask = scheduledExecutor.schedule(() -> limitedUsersMap.remove(user.getId()), cooldown, TimeUnit.MILLISECONDS);
		limitedUsersMap.get(user.getId()).setDeletionTask(deletionTask);
	}

}
