package me.shadorc.discordbot.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

public class JSONUtils {

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
}
