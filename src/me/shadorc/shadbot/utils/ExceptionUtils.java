package me.shadorc.shadbot.utils;

import java.net.SocketTimeoutException;

import org.jsoup.HttpStatusException;

import me.shadorc.shadbot.core.command.Context;

public class ExceptionUtils {

	public static void handle(String action, Context context, Exception err) {
		String msg;
		if(err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503) {
			msg = "Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.";
		} else if(err instanceof SocketTimeoutException) {
			msg = String.format("Mmmh... %s takes too long... This is not my fault, I promise ! Try again later.", StringUtils.capitalize(action));
		} else {
			msg = String.format("Sorry, something went wrong while %s... My developer has been warned.", action);
		}
		LogUtils.errorf(context.getContent(), context.getChannel(), err, msg);
	}

}
