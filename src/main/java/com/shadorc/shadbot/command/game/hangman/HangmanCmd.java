package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class HangmanCmd extends GameCmd<HangmanGame> {

    protected enum Difficulty {
        EASY, HARD;
    }

    private final WordsList easyWords;
    private final WordsList hardWords;

    public HangmanCmd() {
        super(List.of("hangman"));

        this.easyWords = new WordsList("https://gist.githubusercontent.com/" +
                "deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt");
        this.hardWords = new WordsList("https://raw.githubusercontent.com/" +
                "dwyl/english-words/master/words_alpha.txt");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final Difficulty difficulty = Utils.parseEnum(Difficulty.class, context.getArg().orElse("easy"),
                new CommandException(String.format("`%s` is not a valid difficulty. %s",
                        context.getArg().orElse(""), FormatUtils.options(Difficulty.class))));

        return this.loadWords(difficulty)
                .then(Mono.defer(() -> {
                    final HangmanGame hangmanManager = this.getManagers()
                            .putIfAbsent(context.getChannelId(), new HangmanGame(this, context, difficulty));
                    if (hangmanManager == null) {
                        final HangmanGame newHangmanManager = this.getManagers().get(context.getChannelId());
                        return newHangmanManager.start().then(newHangmanManager.show());
                    } else {
                        return context.getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.INFO + " (**%s**) A Hangman game has already been started by **%s**."
                                                        + " Please, wait for him to finish.",
                                                context.getUsername(), hangmanManager.getContext().getUsername()), channel))
                                .then();
                    }
                }));

    }

    private Mono<Void> loadWords(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY && !this.easyWords.isLoaded()) {
            return this.easyWords.load()
                    .then(Mono.fromRunnable(() -> LogUtils.info("Hangman word list (difficulty: easy) obtained.")));
        } else if (difficulty == Difficulty.HARD && !this.hardWords.isLoaded()) {
            return this.hardWords.load()
                    .then(Mono.fromRunnable(() -> LogUtils.info("Hangman word list (difficulty: hard) obtained.")));
        }
        return Mono.empty();
    }

    protected String getWord(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return this.easyWords.getRandomWord();
            case HARD:
                return this.hardWords.getRandomWord();
            default:
                throw new RuntimeException(String.format("Unknown difficulty: %s", difficulty));
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Start a Hangman game.")
                .addArg("difficulty", String.format("%s. The difficulty of the word to find",
                        FormatUtils.format(Difficulty.class, "/")), true)
                .addField("Gains", String.format("The winner gets **%s** plus a bonus (**%s max.**) depending " +
                                "on his number of errors.",
                        FormatUtils.coins(HangmanGame.MIN_GAINS),
                        FormatUtils.coins(HangmanGame.MAX_BONUS)), false)
                .build();
    }
}
