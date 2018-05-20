package me.shadorc.shadbot.utils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.json.JSONException;
import org.jsoup.HttpStatusException;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class ExceptionUtils {

	public static String handleAndGet(String action, Context context, Throwable err) {
		long channelId = context.getChannelId().asLong();

		String msg;
		if(isJsonUnavailable(err) || isUnavailable(err)) {
			msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
			if(isJsonUnavailable(err)) {
				LogUtils.warnf("{Channel ID: %d} %s", channelId, err.getMessage());
			} else {
				LogUtils.warnf("{Channel ID: %d} Service unavailable while %s.", channelId, action);
			}
		}

		else if(isUnreacheable(err)) {
			msg = String.format("Mmmh... %s takes too long... This is not my fault, I promise ! Try again later.", StringUtils.capitalize(action));
			LogUtils.warnf("{Channel ID: %d} A SocketTimeoutException occurred while %s.", channelId, action);
		}

		else {
			msg = String.format("Sorry, something went wrong while %s... My developer has been warned.", action);
			LogUtils.error(context.getContent(), err, String.format("{Channel ID: %d} %s", channelId, msg));
		}

		return Emoji.RED_FLAG + " " + msg;
	}

	private static boolean isJsonUnavailable(Throwable err) {
		return err instanceof JSONException
				|| err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == NetUtils.JSON_ERROR_CODE;
	}

	private static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503;
	}

	private static boolean isUnreacheable(Throwable err) {
		return err instanceof SocketTimeoutException;
	}

}
