package me.shadorc.shadbot.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.shard.CustomShard;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class NetUtils {

	public static final int JSON_ERROR_CODE = 603;

	private static Connection getDefaultConnection(String url) {
		return Jsoup.connect(url)
				.userAgent(Config.USER_AGENT)
				.timeout(Config.DEFAULT_TIMEOUT);
	}

	public static Document getDoc(String url) throws IOException {
		return NetUtils.getDefaultConnection(url).get();
	}

	public static Response getResponse(String url) throws IOException {
		return NetUtils.getDefaultConnection(url)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.execute();
	}

	public static String getBody(String url) throws IOException {
		return NetUtils.getResponse(url).body();
	}

	/**
	 * @param url - the URL from which to obtain the JSON
	 * @return A string representing JSON
	 * @throws HttpStatusException - if the URL returns an invalid JSON
	 */
	public static String getJSON(String url) throws IOException {
		String json = NetUtils.getBody(url);
		if(json.isEmpty() || json.charAt(0) != '{' && json.charAt(0) != '[') {
			throw new HttpStatusException(String.format("%s did not return valid JSON: %s", url, json), JSON_ERROR_CODE, url);
		}
		return json;
	}

	/**
	 * @param str - the string to encode as UTF-8
	 * @return The string encoded as UTF-8
	 * @throws UnsupportedEncodingException
	 */
	public static String encode(String str) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, "UTF-8");
	}

	/**
	 * @param url - a string representing an URL to check
	 * @return true if the string is a valid and reachable URL, false otherwise
	 */
	public static boolean isValidURL(String url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.connect();
			return true;

		} catch (Exception err) {
			return false;

		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}
	}

	public static void postStatsOn(String homeUrl, APIKey token, CustomShard shard) {
		Shadbot.getSelf().subscribe(self -> {
			JSONObject content = new JSONObject()
					.put("shard_id", shard.getIndex())
					.put("shard_count", shard.getShardCount())
					.put("server_count", shard.getGuildsCount());

			String url = String.format("%s/api/bots/%d/stats", homeUrl, self.getId().asLong());
			try {
				Jsoup.connect(url)
						.method(Method.POST)
						.ignoreContentType(true)
						.headers(Map.of("Content-Type", "application/json", "Authorization", APIKeys.get(token)))
						.requestBody(content.toString())
						.post();
			} catch (IOException err) {
				LogUtils.infof("An error occurred while posting statistics of shard %d. (%s: %s).",
						shard.getIndex(), err.getClass().getSimpleName(), err.getMessage());
			}
		});
	}

}
