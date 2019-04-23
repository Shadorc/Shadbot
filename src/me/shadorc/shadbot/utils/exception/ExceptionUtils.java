package me.shadorc.shadbot.utils.exception;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.jsoup.HttpStatusException;

import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ExceptionUtils {

	public static boolean isServerAccessError(Throwable err) {
		return err instanceof HttpStatusException
				|| err instanceof NoRouteToHostException
				|| err instanceof SocketTimeoutException
				|| err instanceof UnknownHostException
				|| err instanceof ConnectException
				|| err instanceof SSLException;
	}

	public static boolean isDiscordForbidden(Throwable err) {
		if(err instanceof ClientException) {
			final ClientException thr = (ClientException) err;
			return thr.getStatus().equals(HttpResponseStatus.FORBIDDEN);
		}
		return false;
	}

}
