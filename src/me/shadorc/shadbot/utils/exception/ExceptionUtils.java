package me.shadorc.shadbot.utils.exception;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ExceptionUtils {

	public static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException
						&& ((HttpStatusException) err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	public static boolean isInternalServerError(Throwable err) {
		return err instanceof HttpStatusException
				&& ((HttpStatusException) err).getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
	}

	public static boolean isUnreacheable(Throwable err) {
		return err instanceof NoRouteToHostException || err instanceof SocketTimeoutException;
	}

	public static boolean isDiscordNotFound(Throwable err) {
		return err instanceof ClientException
				&& ((ClientException) err).getStatus().equals(HttpResponseStatus.NOT_FOUND);
	}

	public static boolean isDiscordForbidden(Throwable err) {
		return err instanceof ClientException
				&& ((ClientException) err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
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
