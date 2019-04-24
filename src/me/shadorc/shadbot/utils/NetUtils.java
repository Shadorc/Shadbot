package me.shadorc.shadbot.utils;

import me.shadorc.shadbot.Config;
import org.apache.http.HttpStatus;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NetUtils {

	/**
	 * @param html - The HTML to convert to text
	 * @return html converted to text with new lines preserved
	 */
	public static String br2nl(String html) {
		if(html == null) {
			return html;
		}
		final Document document = Jsoup.parse(html);
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));// makes html() preserve linebreaks and spacing
		document.select("br").append("\\n");
		document.select("p").prepend("\\n\\n");
		final String str = document.html().replaceAll("\\\\n", "\n");
		return Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
	}

	/**
	 * @param str - the string to encode as UTF-8
	 * @return The string encoded as UTF-8
	 */
	public static String encode(String str) {
		if(str == null || str.isEmpty()) {
			return str;
		}
		return URLEncoder.encode(str, StandardCharsets.UTF_8);
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Connection} corresponding to {@code url} with default user-agent and default timeout
	 */
	public static Connection getDefaultConnection(String url) {
		return Jsoup.connect(url)
				.userAgent(Config.USER_AGENT)
				.timeout(Config.DEFAULT_TIMEOUT);
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Document} corresponding to {@code url} with default user-agent and default timeout
	 * @throws IOException - on error
	 */
	public static Document getDoc(String url) throws IOException {
		return NetUtils.getDefaultConnection(url).get();
	}

	/**
	 * @param url - URL to connect to. The protocol must be http or https
	 * @return The {@link Response} corresponding to {@code url} with default user-agent, default timeout, ignoring content type and HTTP errors
	 * @throws IOException - on error
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
	 * @throws IOException - on error
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
	 * @param url - a string representing an URL to check
	 * @return true if the string is a valid and reachable URL, false otherwise
	 */
	public static boolean isValidUrl(String url) {
		boolean isValid;

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(Config.DEFAULT_TIMEOUT);
			conn.setReadTimeout(Config.DEFAULT_TIMEOUT);
			conn.connect();
			isValid = true;
		} catch (final IOException err) {
			isValid = false;
		}

		if(conn != null) {
			conn.disconnect();
		}

		return isValid;
	}

}
