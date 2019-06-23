package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;

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

    public void load() {
        if (this.words.isEmpty()) {
            this.words.addAll(
                    StringUtils.split(NetUtils.get(this.url).block(), "\n").stream()
                            .filter(word -> NumberUtils.isBetween(word.length(), HangmanCmd.MIN_WORD_LENGTH, HangmanCmd.MAX_WORD_LENGTH))
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
