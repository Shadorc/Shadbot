package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;

public class NetUtils {

	/**
	 * @param url - the String representing URL
	 * @return the Document from url
	 * @throws IOException
	 */
	public static Document getDoc(String url) throws IOException {
		return Jsoup.connect(url)
				.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.91 Safari/537.36 Vivaldi/1.92.917.35")
				.timeout(5000)
				.get();
	}

	/**
	 * @param stringUrl - the String to check
	 * @return true if stringUrl is a valid URL, false otherwise
	 */
	public static boolean isValidURL(String stringUrl) {
		try {
			URL url = new URL(stringUrl);
			URLConnection conn = url.openConnection();
			conn.connect();
		} catch (IOException err) {
			return false;
		}
		return true;
	}

	public static void postStats() {
		if(Config.VERSION.isBeta()) {
			return;
		}
		NetUtils.postStatsOn("https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN);
		NetUtils.postStatsOn("https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN);
	}

	private static void postStatsOn(String homeUrl, APIKey token) {
		DataOutputStream out = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(homeUrl + "/api/bots/" + Shadbot.getClient().getOurUser().getLongID() + "/stats");

			URLConnection urlConn = url.openConnection();
			urlConn.setRequestProperty("Content-Type", "application/json");
			urlConn.setRequestProperty("Authorization", Config.getAPIKey(token));
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setUseCaches(false);

			JSONObject content = new JSONObject().put("server_count", Shadbot.getClient().getGuilds().size());

			out = new DataOutputStream(urlConn.getOutputStream());
			out.writeBytes(content.toString());
			out.flush();

			reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			StringBuilder strBuilder = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				strBuilder.append(line);
			}
			LogUtils.info("Stats have been posted to " + homeUrl + " (Response: " + strBuilder.toString() + ")");

		} catch (IOException err) {
			LogUtils.info("An error occured while posting stats. (" + err.getClass() + ": " + err.getMessage() + ")");

		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(reader);
		}
	}
}
