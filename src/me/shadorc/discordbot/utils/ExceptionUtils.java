package me.shadorc.discordbot.utils;

import me.shadorc.discordbot.utils.schedule.ScheduledMessage.Reason;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;

public class ExceptionUtils {

	public static void manageMessageException(Object message, IChannel channel, DiscordException err) {
		if(err.getErrorMessage().contains("Discord didn't return a response") || err.getErrorMessage().contains("400 Bad Request")) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} A message could not be send now, adding it to queue.");
			Scheduler.scheduleMessages(message, channel, Reason.API_ERROR);

		} else if(err.getErrorMessage().contains("Attempt to send message before shard is ready!")) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} A message could not be send because shard isn't ready, adding it to queue.");
			Scheduler.scheduleMessages(message, channel, Reason.SHARD_NOT_READY);

		} else {
			LogUtils.error("Discord exception while sending message.", err);
		}
	}
}
