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

	public static String getJSON(String url) throws IOException {
		String json = NetUtils.getBody(url);
		if(json.isEmpty() || json.charAt(0) != '{' && json.charAt(0) != '[') {
			throw new HttpStatusException(String.format("%s did not return valid JSON: %s", url, json), JSON_ERROR_CODE, url);
		}
		return json;
	}

	public static String encode(String str) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, "UTF-8");
	}

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

	public static void postStats() {
		LogUtils.infof("Posting statistics...");
		for(IShard shard : Shadbot.getClient().getShards()) {
			NetUtils.postStatsOn("https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN, shard);
			NetUtils.postStatsOn("https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN, shard);
		}
		LogUtils.infof("Statistics posted.");
	}

	private static void postStatsOn(String homeUrl, APIKey token, IShard shard) {
		try {
			JSONObject content = new JSONObject()
					.put("shard_id", shard.getInfo()[0])
					.put("shard_count", shard.getInfo()[1])
					.put("server_count", shard.getGuilds().size());

			String url = String.format("%s/api/bots/%d/stats", homeUrl, shard.getClient().getSelf().getLongID());
			Jsoup.connect(url)
					.method(Method.POST)
					.ignoreContentType(true)
					.headers(Map.of("Content-Type", "application/json", "Authorization", APIKeys.get(token)))
					.requestBody(content.toString())
					.post();
		} catch (Exception err) {
			LogUtils.infof("An error occurred while posting statistics of shard %d. (%s: %s).",
					shard.getInfo()[0], err.getClass().getSimpleName(), err.getMessage());
		}
	}

}
