package me.shadorc.shadbot.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {

	public static <T> List<T> toList(JSONArray array, Class<T> listClass) {
		if(array == null) {
			return null;
		}

		List<T> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			if(listClass.isInstance(array.get(i))) {
				list.add(listClass.cast(array.get(i)));
			} else {
				throw new IllegalArgumentException(String.format("Array's elements cannot be casted to %s.", listClass.getSimpleName()));
			}
		}
		return list;
	}

	public static JSONObject getOrDefault(JSONObject obj, JSONObject defaultObj, String... keys) {
		if(obj == null) {
			return defaultObj;
		}

		JSONObject lastObj = obj;
		for(String key : keys) {
			if(lastObj != null && lastObj.has(key)) {
				lastObj = obj.optJSONObject(key);
			} else {
				return defaultObj;
			}
		}
		return lastObj;
	}

}
