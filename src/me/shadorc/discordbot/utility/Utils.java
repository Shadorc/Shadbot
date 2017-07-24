package me.shadorc.discordbot.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Storage;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

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

	public static String convertToUTF8(String text) {
		return StringEscapeUtils.unescapeHtml3(text);
	}

	public static String formatPlaylist(BlockingQueue <AudioTrack> queue) {
		StringBuilder playlist = new StringBuilder("Musique(s) en attente : ");
		if(queue.isEmpty()) {
			playlist.append("Aucune");
		}
		for(AudioTrack track : queue) {
			playlist.append("\n\t- " + track.getInfo().author + " - " + track.getInfo().title);
		}
		return playlist.toString();
	}

	public static int rand(int bound) {
		return RAND.nextInt(bound);
	}

	public static void gain(IGuild guild, Long authorId, int gain) {
		String coinsStr = Storage.get(guild, authorId);
		int coins = 0;
		if(coinsStr != null) {
			coins = Integer.parseInt(coinsStr);
		}
		Storage.store(guild, authorId, coins+gain);
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

	public static boolean isAdmin(IGuild guild, IUser user) {
		return user.getPermissionsForGuild(guild).contains(Permissions.ADMINISTRATOR);
	}
}