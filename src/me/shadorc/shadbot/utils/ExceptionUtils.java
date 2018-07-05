package me.shadorc.shadbot.utils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.NoMusicException;

public class ExceptionUtils {

	public static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	public static boolean isUnreacheable(Throwable err) {
		return err instanceof SocketTimeoutException;
	}

	public static boolean isForbidden(Throwable err) {
		return err instanceof ClientException && ((ClientException) err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotForbidden(Throwable err) {
		return !ExceptionUtils.isForbidden(err);
	}

	public static boolean isUnknown(Throwable err) {
		return !(err instanceof CommandException)
				&& !(err instanceof MissingArgumentException)
				&& !(err instanceof NoMusicException)
				&& !ExceptionUtils.isUnavailable(err)
				&& !ExceptionUtils.isUnreacheable(err)
				&& !ExceptionUtils.isForbidden(err);
	}

}
