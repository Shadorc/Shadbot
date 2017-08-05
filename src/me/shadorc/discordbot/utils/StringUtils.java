package me.shadorc.discordbot.utils;

import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class StringUtils {

	/**
	 * @param str - String to capitalize
	 * @return str with the first letter capitalized
	 */
	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * @param text - the String to convert, may be null
	 * @return a new converted String, null if null string input
	 */
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
		StringBuilder strBuilder = new StringBuilder();
		if(info.author.equals("Unknown artist")) {
			strBuilder.append(info.title);
		} else {
			strBuilder.append(info.author + " - " + info.title);
		}

		return strBuilder.toString();
	}

	/**
	 * @param str - String to check
	 * @return true if it can be cast to an Integer, false otherwise
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
}
