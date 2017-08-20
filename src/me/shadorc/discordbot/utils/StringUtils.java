package me.shadorc.discordbot.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class StringUtils {

	/**
	 * @param str - the String to capitalize
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

	public static String formatTrackName(AudioTrackInfo info) {
		StringBuilder strBuilder = new StringBuilder();
		if("Unknown artist".equals(info.author)) {
			strBuilder.append(info.title);
		} else {
			strBuilder.append(info.author + " - " + info.title);
		}

		if(info.isStream) {
			strBuilder.append(" (Stream)");
		} else {
			strBuilder.append(" (" + StringUtils.formatDuration(info.length) + ")");
		}

		return strBuilder.toString();
	}

	/**
	 * @param duration - the duration to format
	 * @return the formatted duration
	 */
	public static String formatDuration(long duration) {
		return DurationFormatUtils.formatDuration(duration, "m:ss", true);
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast as an Integer, false otherwise
	 */
	public static boolean isInteger(String str) {
		if(str == null) {
			return false;
		}
		int length = str.length();
		if(length == 0) {
			return false;
		}
		int pos = 0;
		if(str.charAt(0) == '-') {
			if(length == 1) {
				return false;
			}
			pos = 1;
		}
		for(; pos < length; pos++) {
			char charac = str.charAt(pos);
			if(charac < '0' || charac > '9') {
				return false;
			}
		}
		return true;
	}
}
