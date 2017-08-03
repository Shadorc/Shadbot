package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.JSONArray;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import sx.blah.discord.Discord4J;

public class Utils {

	public static String getGlobalInfo() {
		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();

		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long uptime = Duration.between(Discord4J.getLaunchTime().atZone(ZoneId.systemDefault()).toInstant(), Instant.now()).toMillis();

		StringBuilder sb = new StringBuilder();
		sb.append("```css");
		sb.append("\n-= Memory Usage =-");
		sb.append("\nUsed memory: " + format.format((allocatedMemory-freeMemory)/Math.pow(1024, 2)) + "Mb");
		sb.append("\nAllocated memory: " + format.format(allocatedMemory / Math.pow(1024, 2)) + "Mb");
		sb.append("\\n-= APIs Info =-");
		sb.append("\n" + Discord4J.NAME + " Version: " + Discord4J.VERSION);
		sb.append("\nLavaPlayer Version: " + PlayerLibrary.VERSION);
		sb.append("\\n-= Shadbot Info =-");
		sb.append("\nUptime: " + DurationFormatUtils.formatDuration(uptime, "HH:mm:ss", true));
		sb.append("```");

		return sb.toString();
	}

	public static String translate(String langFrom, String langTo, String word) throws IOException {
		String url = "https://translate.googleapis.com/translate_a/single?"+
				"client=gtx&"+
				"sl=" + langFrom +
				"&tl=" + langTo +
				"&dt=t&q=" + URLEncoder.encode(word, "UTF-8");

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		JSONArray jsonArray1 = new JSONArray(response.toString());
		JSONArray jsonArray2 = (JSONArray) jsonArray1.get(0);
		JSONArray jsonArray3 = (JSONArray) jsonArray2.get(0);

		return jsonArray3.get(0).toString();
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(e1, e2) -> e1,
						LinkedHashMap::new
						));
	}
}