package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;

public class NetUtils {

	/**
	 * @param stringUrl - String to check
	 * @return true if stringUrl is a valid URL, false otherwise
	 */
	public static boolean isValidURL(String stringUrl) {
		try {
			URL url = new URL(stringUrl);
			URLConnection conn = url.openConnection();
			conn.connect();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * @return time to send a packet of 32 bytes, -1 if an exception occured
	 */
	public static float getPing() {
		if(Config.VERSION.isBeta()) {
			return -42;
		}

		BufferedReader reader = null;

		try {
			String command = "ping -c 1 www.discordapp.com";

			Process process = Runtime.getRuntime().exec(command);
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			StringBuilder builder = new StringBuilder();

			String line;
			while((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}

			String result = builder.toString();
			String time = result.substring(result.indexOf("time=") + 5, result.indexOf(" ms"));

			return Float.parseFloat(time);

		} catch (IOException e) {
			LogUtils.error("An error occured while parsing ping.", e);

		} finally {
			IOUtils.closeQuietly(reader);
		}

		return -1;
	}

	public static void postStats() {
		if(Config.VERSION.isBeta()) {
			return;
		}
		NetUtils.postStatsOn("https://bots.discord.pw", ApiKeys.BOTS_DISCORD_PW_TOKEN);
		NetUtils.postStatsOn("https://discordbots.org", ApiKeys.DISCORD_BOTS_ORG_TOKEN);
	}

	private static void postStatsOn(String homeUrl, ApiKeys token) {
		DataOutputStream out = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(homeUrl + "/api/bots/" + Shadbot.getClient().getOurUser().getStringID() + "/stats");

			URLConnection urlConn = url.openConnection();
			urlConn.setRequestProperty("Content-Type", "application/json");
			urlConn.setRequestProperty("Authorization", Storage.getApiKey(token));
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

		} catch (IOException e) {
			LogUtils.warn("An error occured while posting stats. (" + e.getClass() + ": " + e.getMessage() + ")");

		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(reader);
		}
	}
}
