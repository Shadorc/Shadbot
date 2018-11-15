package me.shadorc.shadbot.core.exception;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.NoMusicException;

public class ExceptionUtils {

	public static boolean isCommandException(Throwable err) {
		return err instanceof CommandException;
	}

	public static boolean isMissingPermission(Throwable err) {
		return err instanceof MissingPermissionException;
	}

	public static boolean isMissingArgumentException(Throwable err) {
		return err instanceof MissingArgumentException;
	}

	public static boolean isNoMusicException(Throwable err) {
		return err instanceof NoMusicException;
	}

	public static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && ((HttpStatusException) err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	public static boolean isUnreacheable(Throwable err) {
		return err instanceof NoRouteToHostException || err instanceof SocketTimeoutException;
	}

	public static boolean isForbidden(Throwable err) {
		return err instanceof ClientException
				&& ((ClientException) err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotFound(Throwable err) {
		return err instanceof ClientException
				&& ((ClientException) err).getStatus().equals(HttpResponseStatus.NOT_FOUND);
	}

}
