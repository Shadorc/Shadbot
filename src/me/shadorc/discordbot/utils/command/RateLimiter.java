package me.shadorc.discordbot.utils.command;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class RateLimiter {

	public static final int DEFAULT_COOLDOWN = 1;
	public static final int GAME_COOLDOWN = 5;

	private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Boolean>> guildsLimitedUsers;
	private final ScheduledExecutorService executor;
	private final long timeout;

	public RateLimiter(int timeout, ChronoUnit unit) {
		this.timeout = Duration.of(timeout, unit).toMillis();
		this.executor = Executors.newSingleThreadScheduledExecutor(Utils.getThreadFactoryNamed("Shadbot-RateLimiter@" + this.hashCode()));
		this.guildsLimitedUsers = new ConcurrentHashMap<>();
	}

	public boolean isSpamming(IChannel channel, IUser user) {
		if(this.isLimited(channel.getGuild(), user)) {
			if(!this.isWarned(channel.getGuild(), user)) {
				this.warn(channel, user);
			}
			return true;
		}
		return false;
	}

	public boolean isLimited(IGuild guild, IUser user) {
		guildsLimitedUsers.putIfAbsent(guild.getLongID(), new ConcurrentHashMap<>());

		Map<Long, Boolean> guildMap = guildsLimitedUsers.get(guild.getLongID());

		if(guildMap.containsKey(user.getLongID())) {
			return true;
		}

		guildMap.put(user.getLongID(), false);
		executor.schedule(() -> {
			guildMap.remove(user.getLongID());
			if(guildMap.isEmpty()) {
				guildsLimitedUsers.remove(guild.getLongID());
			}
		}, timeout, TimeUnit.MILLISECONDS);
		return false;
	}

	private boolean isWarned(IGuild guild, IUser user) {
		return guildsLimitedUsers.get(guild.getLongID()).get(user.getLongID());
	}

	private void warn(IChannel channel, IUser user) {
		guildsLimitedUsers.get(channel.getGuild().getLongID()).put(user.getLongID(), true);
		BotUtils.sendMessage(Emoji.STOPWATCH + " (**" + user.getName() + "**) " + TextUtils.getSpamMessage() + " You can use this"
				+ " command once every *" + Duration.of(timeout, ChronoUnit.MILLIS).getSeconds() + " sec*.", channel);
	}
}