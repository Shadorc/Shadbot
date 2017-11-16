package me.shadorc.discordbot.utils;

import java.net.SocketTimeoutException;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.schedule.ScheduledMessage.Reason;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;

public class ExceptionUtils {

	public static void manageMessageException(Object message, IChannel channel, DiscordException err) {
		if(err.getErrorMessage().contains("Discord didn't return a response") || err.getErrorMessage().contains("400 Bad Request")) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} A message could not be sent now, adding it to queue.");
			Scheduler.scheduleMessage(message, channel, Reason.API_ERROR);

		} else if(err.getErrorMessage().contains("Attempt to send message before shard is ready!")) {
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} A message could not be sent because shard isn't ready, "
					+ "adding it to queue.");
			Scheduler.scheduleMessage(message, channel, Reason.SHARD_NOT_READY);

		} else {
			LogUtils.error("Discord exception while sending message.", err);
		}
	}

	public static void manageException(String action, Context context, Exception err) {
		if(err instanceof SocketTimeoutException) {
			LogUtils.error("Mmmh... " + StringUtils.capitalize(action) + " takes too long... This is not my fault, I promise ! "
					+ "Try again later.", err, context);
		} else {
			LogUtils.error("Sorry, something went wrong while " + action + "... I will try to fix this as soon as possible.", err, context);
		}
	}
}
