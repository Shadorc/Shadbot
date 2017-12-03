package me.shadorc.discordbot.command.game.hangman;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONException;

import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;

public class HangmanUtils {

	@SuppressWarnings("ucd")
	private static List<String> words;

	static {
		try {
			words = Arrays.stream(NetUtils.getBody("https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt").split("\n"))
					.filter(word -> word.length() >= HangmanManager.MIN_WORD_LENGTH && word.length() <= HangmanManager.MAX_WORD_LENGTH)
					.map(word -> word.trim())
					.collect(Collectors.toList());
		} catch (JSONException | IOException err) {
			LogUtils.error("{ " + HangmanUtils.class.getSimpleName() + "} An error occurred while getting words list.", err);
		}
	}

	protected static String getWord() throws IOException {
		if(words == null || words.isEmpty()) {
			throw new IOException("Words list has not been initialized.");
		}

		return words.get(MathUtils.rand(words.size()));
	}

	protected static String getRepresentation(String word, List<String> charsTested) {
		StringBuilder strBuilder = new StringBuilder();
		for(int i = 0; i < word.length(); i++) {
			if(charsTested.contains(Character.toString(word.charAt(i)))) {
				strBuilder.append("**" + Character.toUpperCase(word.charAt(i)) + "** ");
			} else {
				strBuilder.append("\\_ ");
			}
		}
		return strBuilder.toString();
	}
}
