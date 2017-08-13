package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {

	/**
	 * @param array - JSONArray to convert
	 * @return List containing array elements
	 */
	public static List<String> convertArrayToList(JSONArray array) {
		List<String> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			list.add(array.getString(i));
		}
		return list;
	}

	/**
	 * @param url - webpage's url
	 * @return JSON Object from URL
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject getJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

			StringBuilder sb = new StringBuilder();
			int cp;
			while((cp = reader.read()) != -1) {
				sb.append((char) cp);
			}

			return new JSONObject(sb.toString());

		} finally {
			is.close();
		}
	}
}
