package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), Charset.forName("UTF-8")));

			StringBuilder strBuilder = new StringBuilder();
			int charac;
			while((charac = reader.read()) != -1) {
				strBuilder.append((char) charac);
			}

			return new JSONObject(strBuilder.toString());

		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
}
