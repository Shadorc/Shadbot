package me.shadorc.discordbot.utils;

import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class StringUtils {

	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static String convertHtmlToUTF8(String text) {
		return StringEscapeUtils.unescapeHtml3(text);
	}

	public static String formatPlaylist(BlockingQueue<AudioTrack> queue) {
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
}
