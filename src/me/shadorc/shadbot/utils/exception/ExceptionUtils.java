package me.shadorc.shadbot.utils.exception;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.jsoup.HttpStatusException;

import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ExceptionUtils {

	public static boolean isServerAccessError(Throwable err) {
		return err instanceof HttpStatusException
				|| err instanceof NoRouteToHostException
				|| err instanceof SocketTimeoutException
				|| err instanceof UnknownHostException
				|| err instanceof ConnectException;
	}

	public static boolean isDiscordForbidden(Throwable err) {
		if(err instanceof ClientException) {
			ClientException thr = (ClientException) err;
			return thr.getStatus().equals(HttpResponseStatus.FORBIDDEN);
		}
		return false;
	}

	public static boolean isKnownDiscordError(Throwable err) {
		if(err instanceof ClientException) {
			final HttpResponseStatus status = ((ClientException) err).getStatus();
			return status.equals(HttpResponseStatus.INTERNAL_SERVER_ERROR)
					|| status.equals(HttpResponseStatus.FORBIDDEN)
					|| status.equals(HttpResponseStatus.NOT_FOUND);
		}
		return false;
	}

}
