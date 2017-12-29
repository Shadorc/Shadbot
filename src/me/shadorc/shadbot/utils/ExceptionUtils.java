package me.shadorc.shadbot.utils;

import java.net.SocketTimeoutException;

import org.jsoup.HttpStatusException;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.LogUtils;

public class ExceptionUtils {

	public static void handle(String action, Context context, Exception err) {
		if(err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == 503) {
			LogUtils.errorf(context, err, 
					"Mmmh... This service is currently unavailable... This is not my fault, I promise ! Try again later.");
		} else if(err instanceof SocketTimeoutException) {
			LogUtils.errorf(context, err,
					"Mmmh... " + StringUtils.capitalize(action) + " takes too long... This is not my fault, I promise ! Try again later.");
		} else {
			LogUtils.errorf(context, err, 
					"Sorry, something went wrong while " + action + "... I will try to fix this as soon as possible.");
		}
	}

}
