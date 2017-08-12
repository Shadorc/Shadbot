package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.discordbot.MissingArgumentException;

public class Utils {

	public static List<String> convertArrayToList(JSONArray array) {
		List<String> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			list.add(array.getString(i));
		}
		return list;
	}

	public static String translate(String langFrom, String langTo, String sourceText) throws MissingArgumentException, IOException {
		BufferedReader in = null;
		try {
			URL url = new URL("https://translate.googleapis.com/translate_a/single?" +
					"client=gtx&" +
					"sl=" + langFrom +
					"&tl=" + langTo +
					"&dt=t&q=" + URLEncoder.encode(sourceText, "UTF-8"));

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");

			in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			StringBuilder response = new StringBuilder();

			String inputLine;
			while((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			JSONArray result = new JSONArray(response.toString());
			if(result.get(0) instanceof JSONArray) {
				return ((JSONArray) ((JSONArray) result.get(0)).get(0)).get(0).toString();
			} else {
				throw new MissingArgumentException();
			}

		} catch (IOException e) {
			throw e;

		} finally {
			if(in != null) {
				in.close();
			}
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(e1, e2) -> e1,
						LinkedHashMap::new));
	}
}