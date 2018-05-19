package me.shadorc.shadbot.utils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.json.JSONException;
import org.jsoup.HttpStatusException;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class ExceptionUtils {

	public static void handle(String action, Context context, Throwable err) {
		context.getChannel().subscribe(channel -> {
			long channelId = channel.getId().asLong();

			String msg;
			if(err instanceof JSONException || err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == NetUtils.JSON_ERROR_CODE) {
				msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
				LogUtils.warnf("{Channel ID: %d} %s", channelId, err.getMessage());
			}

			else if(err instanceof ConnectException || err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503) {
				msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
				LogUtils.warnf("{Channel ID: %d} Service unavailable while %s.", channelId, action);
			}

			else if(err instanceof SocketTimeoutException) {
				msg = String.format("Mmmh... %s takes too long... This is not my fault, I promise ! Try again later.", StringUtils.capitalize(action));
				LogUtils.warnf("{Channel ID: %d} A SocketTimeoutException occurred while %s.", channelId, action);
			}

			else {
				msg = String.format("Sorry, something went wrong while %s... My developer has been warned.", action);
				LogUtils.error(context.getContent(), err, String.format("{Channel ID: %d} %s", channelId, msg));
			}

			BotUtils.sendMessage(Emoji.RED_FLAG + " " + msg, channel);
		});
	}

}
