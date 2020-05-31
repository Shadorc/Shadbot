package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.RandUtils;
import com.shadorc.shadbot.utils.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class WordsList {

    private static final int MIN_WORD_LENGTH = 5;
    private static final int MAX_WORD_LENGTH = 10;

    private final String url;
    private final List<String> words;

    public WordsList(String url) {
        this.url = url;
        this.words = new ArrayList<>();
    }

    public Mono<Void> load() {
        if (this.words.isEmpty()) {
            return NetUtils.get(this.url)
                    .flatMapIterable(str -> StringUtils.split(str, "\n"))
                    .filter(word -> NumberUtils.isBetween(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
                    .take(500)
                    .collectList()
                    .doOnNext(this.words::addAll)
                    .then();
        }
        return Mono.empty();
    }

    public String getRandomWord() {
        return RandUtils.randValue(this.words);
    }

    public boolean isLoaded() {
        return !this.words.isEmpty();
    }

}
