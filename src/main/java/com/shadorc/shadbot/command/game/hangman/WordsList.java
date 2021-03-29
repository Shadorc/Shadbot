package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.StringUtil;
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

    public Mono<List<String>> load() {
        if (this.isLoaded()) {
            return Mono.empty();
        }

        return RequestHelper.request(this.url)
                .flatMapIterable(str -> StringUtil.split(str, "\n"))
                .filter(word -> NumberUtil.isBetween(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
                .take(500)
                .collectList()
                .doOnNext(this.words::addAll);
    }

    public String getRandomWord() {
        return RandUtil.randValue(this.words);
    }

    public boolean isLoaded() {
        return !this.words.isEmpty();
    }

}
