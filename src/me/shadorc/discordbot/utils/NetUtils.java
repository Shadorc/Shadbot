package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Log;
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

		BufferedReader in = null;

		try {
			String command = "ping -c 1 www.discordapp.com";

			Process p = Runtime.getRuntime().exec(command);
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));

			StringBuilder builder = new StringBuilder();

			String line;
			while((line = in.readLine()) != null) {
				builder.append(line + "\n");
			}

			String result = builder.toString();
			String time = result.substring(result.indexOf("time=") + 5, result.indexOf(" ms"));

			return Float.parseFloat(time);

		} catch (Exception e) {
			Log.error("An error occured while parsing ping.", e);

		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.error("Error while closing reader during ping parsing.", e);
				}
			}
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
		BufferedReader in = null;
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

			in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			String s;
			while((s = in.readLine()) != null) {
				Log.info(homeUrl + " response while posting stats: " + s);
			}
		} catch (Exception e) {
			Log.error("An error occured while posting stats.", e);
		} finally {
			try {
				if(out != null) {
					out.close();
				}
				if(in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.error("An error occured while posting stats.", e);
			}
		}
	}
}
