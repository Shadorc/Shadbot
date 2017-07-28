package me.shadorc.discordbot.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import me.shadorc.discordbot.Storage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Utils {

	private static final Random RAND = new Random();

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

	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static String convertToUTF8(String text) {
		return StringEscapeUtils.unescapeHtml3(text);
	}

	public static List<String> convertToList(JSONArray jArray) {
		List<String> list = new ArrayList<>();
		for(int i = 0; i < jArray.length(); i++) {
			list.add(jArray.getString(i).toLowerCase());
		}
		return list;
	}

	public static String formatPlaylist(BlockingQueue <AudioTrack> queue) {
		StringBuilder playlist = new StringBuilder(queue.size() + " musique(s) en attente");
		if(queue.isEmpty()) {
			playlist.append("Aucune");
		}
		for(AudioTrack track : queue) {
			String name = "\n\t- " + track.getInfo().author + " - " + track.getInfo().title;
			if(playlist.length() + name.length() < 2000) {
				playlist.append(name);
			}
		}
		return playlist.toString();
	}

	public static String formatTrackName(AudioTrackInfo info) {
		return (info.author.equals("Unknown artist") ? "" : (info.author + " - ")) + info.title;
	}

	/**
	 *
	 * @param min - min value
	 * @param max - max value
	 * @return rand number between min (inclusive) and max (inclusive)
	 */
	public static int rand(int min, int max) {
		return min + Utils.rand(max-min+1);
	}

	public static int rand(int bound) {
		return RAND.nextInt(bound);
	}

	public static void addCoins(IGuild guild, IUser user, int gain) {
		int coins = Storage.getCoins(guild, user);
		Storage.storeCoins(guild, user, coins+gain);
	}

	public static int getLevenshteinDistance(String word1, String word2) {
		int[][] distance = new int[word1.length() + 1][word2.length() + 1];

		for (int i = 0; i <= word1.length(); i++) {
			distance[i][0] = i;
		}
		for (int j = 1; j <= word2.length(); j++) {
			distance[0][j] = j;
		}

		for (int i = 1; i <= word1.length(); i++) {
			for (int j = 1; j <= word2.length(); j++) {
				distance[i][j] = Math.min(
						Math.min(
								distance[i - 1][j] + 1,
								distance[i][j - 1] + 1),
						distance[i - 1][j - 1] + ((word1.charAt(i - 1) == word2.charAt(j - 1)) ? 0 : 1));
			}
		}

		return distance[word1.length()][word2.length()];
	}

	/**
	 * @param stringUrl
	 * @return true if stringUrl is a valid URL
	 */
	public static boolean isValidURL(String stringUrl) {
		try {
			URL url = new URL(stringUrl);
			URLConnection conn = url.openConnection();
			conn.connect();
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * @return true if Shadbot is allowed to post to the channel in this guild
	 */
	public static boolean isChannelAllowed(IGuild guild, IChannel channel) {
		JSONArray channelsArray = Storage.getAllowedChannels(guild);
		//If no permissions were defined, authorize by default all the channels
		if(channelsArray == null) {
			return true;
		}
		for(int i = 0; i < channelsArray.length(); i++) {
			if(channelsArray.get(i).equals(channel.getStringID())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param str - String to check
	 * @return true if str can be cast to an Integer, false otherwise
	 */
	public static boolean isInteger(String str) {
		if(str == null) {
			return false;
		}
		int length = str.length();
		if(length == 0) {
			return false;
		}
		int i = 0;
		if(str.charAt(0) == '-') {
			if(length == 1) {
				return false;
			}
			i = 1;
		}
		for(; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
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