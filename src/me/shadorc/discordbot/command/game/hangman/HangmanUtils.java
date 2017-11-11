package me.shadorc.discordbot.command.game.hangman;

import java.io.IOException;
import java.util.List;

import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;

public class HangmanUtils {

	protected static String getWord(int minWordLength, int maxWordLength) throws IOException {
		String word;
		do {
			word = NetUtils.getBody("http://setgetgo.com/randomword/get.php");
		} while(word.length() <= minWordLength || word.length() >= maxWordLength || StringUtils.capitalize(word).equals(word));
		return word;
	}

	protected static String getRepresentation(String word, List<String> charsTested) {
		StringBuilder strBuilder = new StringBuilder();
		for(int i = 0; i < word.length(); i++) {
			if(i == 0 || i == word.length() - 1 || charsTested.contains(Character.toString(word.charAt(i)))) {
				strBuilder.append("**" + Character.toUpperCase(word.charAt(i)) + "** ");
			} else {
				strBuilder.append("\\_ ");
			}
		}
		return strBuilder.toString();
	}
}
