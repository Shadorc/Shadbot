package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class HangmanCmd extends GameCmd<HangmanGame> {

    protected enum Difficulty {
        EASY, HARD
    }

    private final WordsList easyWords;
    private final WordsList hardWords;

    public HangmanCmd() {
        super("hangman", "Start a Hangman game");
        this.addOption(option -> option.name("difficulty")
                .description("The difficulty of the word to find, easy by default")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Difficulty.class)));

        this.easyWords = new WordsList(
                "https://gist.githubusercontent.com/deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt");
        this.hardWords = new WordsList(
                "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt");
    }

    @Override
    public Mono<?> execute(Context context) {
        final Difficulty difficulty = context.getOptionAsEnum(Difficulty.class, "difficulty").orElse(Difficulty.EASY);
        return this.loadWords(difficulty)
                .then(Mono.defer(() -> {
                    if (this.getManagers().containsKey(context.getChannelId())) {
                        return context.reply(Emoji.INFO, context.localize("hangman.already.started"));
                    }

                    final HangmanGame game = new HangmanGame(this, context, difficulty);
                    this.getManagers().put(context.getChannelId(), game);
                    return game.start()
                            .then(game.show())
                            .doOnError(err -> this.getManagers().remove(context.getChannelId()));
                }));

    }

    private Mono<List<String>> loadWords(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY && !this.easyWords.isLoaded()) {
            return this.easyWords.load()
                    .doOnSuccess(__ -> DEFAULT_LOGGER.info("Hangman word list (difficulty: easy) loaded"));
        } else if (difficulty == Difficulty.HARD && !this.hardWords.isLoaded()) {
            return this.hardWords.load()
                    .doOnSuccess(__ -> DEFAULT_LOGGER.info("Hangman word list (difficulty: hard) loaded"));
        }
        return Mono.empty();
    }

    protected WordsList getEasyWords() {
        return this.easyWords;
    }

    protected WordsList getHardWords() {
        return this.hardWords;
    }

}