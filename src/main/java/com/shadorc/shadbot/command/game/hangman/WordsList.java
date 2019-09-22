package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class WordsList {

    private final String url;
    private final List<String> words;

    public WordsList(String url) {
        this.url = url;
        this.words = new ArrayList<>();
    }

    public Mono<Void> load() {
        if (this.words.isEmpty()) {
            return NetUtils.get(this.url)
                    .map(str -> StringUtils.split(str, "\n"))
                    .flatMapMany(Flux::fromIterable)
                    .filter(word -> NumberUtils.isBetween(word.length(), HangmanCmd.MIN_WORD_LENGTH, HangmanCmd.MAX_WORD_LENGTH))
                    .take(500)
                    .collectList()
                    .doOnNext(this.words::addAll)
                    .then();
        }
        return Mono.empty();
    }

    public String getRandomWord() {
        return Utils.randValue(this.words);
    }

    public boolean isLoaded() {
        return !this.words.isEmpty();
    }

}
