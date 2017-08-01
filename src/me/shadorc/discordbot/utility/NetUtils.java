package me.shadorc.discordbot.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

	/**
	 * @param  url - webpage's url
	 * @return Whole HTML from the URL
	 * @throws IOException
	 */
	public static String getHTML(URL url) throws IOException {
		BufferedReader reader = null;
		try {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.75 Safari/537.36 Vivaldi/1.0.219.50");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.connect();

			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			StringBuilder html = new StringBuilder();

			String line;
			while((line = reader.readLine()) != null) {
				html.append(line + "\n");
			}

			return html.toString();

		} finally {
			if(reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * @param  text - text to parse
	 * @param  start - starting String
	 * @param  end - ending String
	 * @return All substrings between start and end Strings in text
	 */
	public static ArrayList <String> getAllSubstring(String text, String start, String end) {
		ArrayList <String> lines = new ArrayList<>();
		Pattern p = Pattern.compile(Pattern.quote(start) + "(?s)(.*?)" + Pattern.quote(end));
		Matcher m = p.matcher(text);
		while(m.find()) {
			lines.add(m.group(1));
		}

		return lines;
	}

	/**
	 * @param  url - webpage's url
	 * @param  toMatch - String to match in HTML code
	 * @param  start - parsing begin
	 * @param  end - parsing ending
	 * @return Parsed HTML from "start" to "end"
	 * @throws IOException
	 */
	public static String parseHTML(URL url, String toMatch, String start, String end) throws IOException {
		BufferedReader reader = null;
		try {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.connect();

			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			String line;
			while((line = reader.readLine()) != null) {
				if(line.contains(toMatch)) {
					Pattern p = Pattern.compile(Pattern.quote(start) + "(.*?)" + Pattern.quote(end));
					Matcher m = p.matcher(line);
					if(m.find()) {
						return m.group(1).trim();
					}
				}
			}

		} finally {
			if(reader != null) {
				reader.close();
			}
		}

		return null;
	}

	/**
	 * @param  html - webpage's html
	 * @param  toMatch - String to match in HTML code
	 * @param  start - parsing begin
	 * @param  end - parsing ending
	 * @return Parsed HTML from "start" to "end"
	 */
	public static String parseTextHTML(String html, String toMatch, String start, String end) {
		for(String line : html.split("\n")) {
			if(line.contains(toMatch)) {
				Pattern p = Pattern.compile(Pattern.quote(start) + "(.*?)" + Pattern.quote(end));
				Matcher m = p.matcher(line);
				if(m.find()) {
					return m.group(1).trim();
				}
			}
		}

		return null;
	}

	/**
	 * @return Average time to send 1 packet of 32 bytes
	 * @throws IOException
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
			average = Integer.parseInt(array[array.length-1].replaceAll("[^\\d]", ""));

			return average;

		} catch (IOException e) {
			Log.error("Error while parsing ping.", e);
			e.printStackTrace();

		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.error("Error while parsing ping.", e);
					e.printStackTrace();
				}
			}
		}

		return -1;
	}
}