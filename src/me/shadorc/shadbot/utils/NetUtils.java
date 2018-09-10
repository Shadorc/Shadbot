package me.shadorc.shadbot.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import discord4j.core.DiscordClient;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class NetUtils {

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Connection} corresponding to {@code url} with default user-agent and default timeout
	 */
	private static Connection getDefaultConnection(String url) {
		return Jsoup.connect(url)
				.userAgent(Config.USER_AGENT)
				.timeout(Config.DEFAULT_TIMEOUT);
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Document} corresponding to {@code url} with default user-agent and default timeout
	 * @throws IOException
	 */
	public static Document getDoc(String url) throws IOException {
		return NetUtils.getDefaultConnection(url).get();
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Response} corresponding to {@code url} with default user-agent, default timeout, ignoring content type and HTTP errors
	 * @throws IOException
	 */
	public static Response getResponse(String url) throws IOException {
		return NetUtils.getDefaultConnection(url)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.execute();
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@code body} corresponding to the {@code url} with default user-agent and default timeout
	 * @throws IOException
	 */
	public static String getBody(String url) throws IOException {
		return NetUtils.getResponse(url).body();
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return A string representing JSON
	 * @throws HttpStatusException if the URL returns an invalid JSON
	 */
	public static String getJSON(String url) throws IOException {
		final String json = NetUtils.getBody(url);
		if(json.isEmpty() || json.charAt(0) != '{' && json.charAt(0) != '[') {
			final String errorMessage = Jsoup.parse(json).text();
			throw new HttpStatusException(
					String.format("%s did not return valid JSON: %s", url, errorMessage.isEmpty() ? "Empty" : errorMessage),
					HttpStatus.SC_SERVICE_UNAVAILABLE,
					url);
		}
		return json;
	}

	/**
	 * @param str - the string to encode as UTF-8
	 * @return The string encoded as UTF-8
	 * @throws UnsupportedEncodingException If the named encoding is not supported
	 */
	public static String encode(String str) throws UnsupportedEncodingException {
		if(str == null || str.isEmpty()) {
			return str;
		}
		return URLEncoder.encode(str, "UTF-8");
	}

	/**
	 * @param url - a string representing an URL to check
	 * @return true if the string is a valid and reachable URL, false otherwise
	 */
	public static boolean isValidUrl(String url) {
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

	/**
	 * @param client - the client from which to post statistics
	 */
	public static Mono<Void> postStats(DiscordClient client) {
		if(Config.IS_SNAPSHOT) {
			return Mono.empty();
		}
		return Mono.fromRunnable(() -> LogUtils.infof("{Shard %d} Posting statistics...", client.getConfig().getShardIndex()))
				.then(NetUtils.postStatsOn(client, "https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN))
				.then(NetUtils.postStatsOn(client, "https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN))
				.then(Mono.fromRunnable(() -> LogUtils.infof("{Shard %d} Statistics posted.", client.getConfig().getShardIndex())));
	}

	/**
	 * @param homeUrl - the statistics site URL
	 * @param token - the API token corresponding to the website
	 * @param client - the client from which to post statistics
	 */
	private static Mono<Void> postStatsOn(DiscordClient client, String homeUrl, APIKey token) {
		return client.getGuilds().count()
				.doOnSuccess(guildsCount -> {
					final JSONObject content = new JSONObject()
							.put("shard_id", client.getConfig().getShardIndex())
							.put("shard_count", client.getConfig().getShardCount())
							.put("server_count", guildsCount);
					final String url = String.format("%s/api/bots/%d/stats", homeUrl, client.getSelfId().get().asLong());

					try {
						Jsoup.connect(url)
								.method(Method.POST)
								.ignoreContentType(true)
								.headers(Map.of("Content-Type", "application/json", "Authorization", APIKeys.get(token)))
								.requestBody(content.toString())
								.post();
					} catch (IOException err) {
						Exceptions.propagate(err);
					}
				})
				.then();
	}

}
