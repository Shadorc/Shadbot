package me.shadorc.discordbot.utils.command;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TextUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	public static final int COMMON_COOLDOWN = 2;
	public static final int GAME_COOLDOWN = 5;

	private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, LimitedUser>> guildsLimitedUsers;
	private final long timeout;

	private class LimitedUser {
		private long lastTime;
		private boolean isWarned;

		protected LimitedUser() {
			this.lastTime = 0;
			this.isWarned = false;
		}

		public long getLastTime() {
			return lastTime;
		}

		public void setLastTime(long lastTime) {
			this.lastTime = lastTime;
		}

		public void setWarned(boolean isWarned) {
			this.isWarned = isWarned;
		}

		public boolean isWarned() {
			return isWarned;
		}
	}

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.guildsLimitedUsers = new ConcurrentHashMap<>();
	}

	public boolean isSpamming(Context context) {
		if(this.isLimited(context.getGuild(), context.getAuthor())) {
			if(!this.isWarned(context.getGuild(), context.getAuthor())) {
				this.warn(context);
			}
			return true;
		}
		return false;
	}

	private boolean isLimited(IGuild guild, IUser user) {
		guildsLimitedUsers.putIfAbsent(guild.getLongID(), new ConcurrentHashMap<>());
		LimitedUser limitedUser = guildsLimitedUsers.get(guild.getLongID()).getOrDefault(user.getLongID(), new LimitedUser());

		long diff = System.currentTimeMillis() - limitedUser.getLastTime();
		if(diff > timeout) {
			limitedUser.setLastTime(System.currentTimeMillis());
			limitedUser.setWarned(false);
			guildsLimitedUsers.get(guild.getLongID()).put(user.getLongID(), limitedUser);
			return false;
		}
		return true;
	}

	private boolean isWarned(IGuild guild, IUser user) {
		return guildsLimitedUsers.get(guild.getLongID()).get(user.getLongID()).isWarned();
	}

	private void warn(Context context) {
		BotUtils.sendMessage(Emoji.STOPWATCH + " " + TextUtils.getSpamMessage() + " You can use this command once every "
				+ Duration.of(timeout, ChronoUnit.MILLIS).getSeconds() + " sec.", context.getChannel());
		guildsLimitedUsers.get(context.getGuild().getLongID()).get(context.getAuthor().getLongID()).setWarned(true);
		Stats.increment(StatCategory.LIMITED_COMMAND, context.getCommand());
	}
}