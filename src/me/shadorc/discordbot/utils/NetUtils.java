package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

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
	public static int getPing() {
		BufferedReader in = null;

		try {
			String command = "ping -n 1 www.discordapp.com";

			Process p = Runtime.getRuntime().exec(command);
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));

			StringBuilder builder = new StringBuilder();

			String line;
			while((line = in.readLine()) != null) {
				builder.append(line + "\n");
			}

			int average = 0;
			String[] array = builder.toString().split(" = ");
			average = Integer.parseInt(array[array.length - 1].replaceAll("[^\\d]", ""));

			return average;

		} catch (IOException e) {
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

}
