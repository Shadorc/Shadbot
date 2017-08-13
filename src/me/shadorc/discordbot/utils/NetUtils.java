package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONObject;

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
		DataOutputStream printout = null;
		try {
			URL url = new URL("https://bots.discord.pw/api/bots/" + Shadbot.getClient().getOurUser().getStringID() + "/stats");

			URLConnection urlConn = url.openConnection();
			urlConn.setRequestProperty("Content-Type", "application/json");
			urlConn.setRequestProperty("Authorization", Storage.getApiKey(ApiKeys.DISCORD_BOTS_TOKEN));
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);

			JSONObject content = new JSONObject().put("server_count", Shadbot.getClient().getGuilds().size());

			printout = new DataOutputStream(urlConn.getOutputStream());
			printout.writeBytes(content.toString());
			printout.flush();
		} catch (Exception ignored) {
			// Ignored
		} finally {
			try {
				if(printout != null) {
					printout.close();
				}
			} catch (Exception ignored) {
				// Ignored
			}
		}
	}

}
