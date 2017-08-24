package me.shadorc.discordbot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import me.shadorc.discordbot.MissingArgumentException;

public class Utils {

	public static String translate(String langFrom, String langTo, String sourceText) throws MissingArgumentException, IOException {
		BufferedReader reader = null;
		try {
			URL url = new URL("https://translate.googleapis.com/translate_a/single?" +
					"client=gtx&" +
					"sl=" + langFrom +
					"&tl=" + langTo +
					"&dt=t&q=" + URLEncoder.encode(sourceText, "UTF-8"));

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.91 Safari/537.36 Vivaldi/1.92.917.35");

			reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			StringBuilder response = new StringBuilder();

			String inputLine;
			while((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}

			JSONArray result = new JSONArray(response.toString());
			if(result.get(0) instanceof JSONArray) {
				return ((JSONArray) ((JSONArray) result.get(0)).get(0)).get(0).toString();
			} else {
				throw new MissingArgumentException();
			}

		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(value1, value2) -> value1,
						LinkedHashMap::new));
	}

	/**
	 * @return double representing process CPU load percentage value, Double.NaN if not available
	 */
	public static double getProcessCpuLoad() {
		double cpuLoad;
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if(list.isEmpty()) {
				return Double.NaN;
			}

			Attribute att = (Attribute) list.get(0);
			Double value = (Double) att.getValue();

			if(value == -1.0) {
				return Double.NaN;
			}

			cpuLoad = value * 100d;
		} catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException e) {
			cpuLoad = Double.NaN;
		}

		return cpuLoad;
	}

	/**
	 * @param array - JSONArray to convert
	 * @return List<String> containing array elements
	 */
	public static List<String> convertArrayToList(JSONArray array) {
		if(array == null) {
			return null;
		}
		List<String> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			list.add(array.getString(i));
		}
		return list;
	}

	public static boolean allEqual(Object key, Object... objs) {
		for(Object obj : objs) {
			if(!obj.equals(key)) {
				return false;
			}
		}
		return true;
	}
}