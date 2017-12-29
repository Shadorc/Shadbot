package me.shadorc.shadbot.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import me.shadorc.shadbot.Config;

public class NetUtils {

	public static Response getResponse(String url) throws IOException {
		return Jsoup.connect(url)
				.userAgent(Config.USER_AGENT)
				.timeout(Config.DEFAULT_TIMEOUT)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.execute();
	}

	public static String getBody(String url) throws IOException {
		return NetUtils.getResponse(url).body();
	}

	public static String encode(String str) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, "UTF-8");
	}

}
