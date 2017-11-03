package me.shadorc.discordbot.utils.command;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TextUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	public static final int COMMON_COOLDOWN = 2;
	public static final int GAME_COOLDOWN = 5;

	protected final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Boolean>> guildsLimitedUsers;
	private final Timer timer;
	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.timer = new Timer();
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

		Map<Long, Boolean> guildMap = guildsLimitedUsers.get(guild.getLongID());

		if(guildMap.containsKey(user.getLongID())) {
			return true;
		}

		guildMap.put(user.getLongID(), false);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				guildMap.remove(user.getLongID());
				if(guildMap.isEmpty()) {
					guildsLimitedUsers.remove(guild.getLongID());
				}
			}
		}, timeout);
		return false;
	}

	private boolean isWarned(IGuild guild, IUser user) {
		return guildsLimitedUsers.get(guild.getLongID()).get(user.getLongID());
	}

	private void warn(Context context) {
		BotUtils.sendMessage(Emoji.STOPWATCH + " (**" + context.getAuthorName() + "**) " + TextUtils.getSpamMessage() + " You can use this"
				+ " command once every *" + Duration.of(timeout, ChronoUnit.MILLIS).getSeconds() + " sec*.", context.getChannel());
		guildsLimitedUsers.get(context.getGuild().getLongID()).put(context.getAuthor().getLongID(), true);
		StatsManager.increment(StatCategory.LIMITED_COMMAND, context.getCommand());
	}
}