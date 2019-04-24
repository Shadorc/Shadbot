package me.shadorc.shadbot.command.game.hangman;

import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WordsList {

    private final String url;
    private final List<String> words;

    public WordsList(String url) {
        this.url = url;
        this.words = new ArrayList<>();
    }

    public void load() throws IOException {
        if (this.words.isEmpty()) {
            this.words.addAll(
                    StringUtils.split(NetUtils.getBody(this.url), "\n").stream()
                            .filter(word -> NumberUtils.isInRange(word.length(), HangmanCmd.MIN_WORD_LENGTH, HangmanCmd.MAX_WORD_LENGTH))
                            .limit(500)
                            .collect(Collectors.toList()));
        }
    }

    public String getRandomWord() {
        return Utils.randValue(this.words);
    }

    public boolean isLoaded() {
        return !this.words.isEmpty();
    }

}
