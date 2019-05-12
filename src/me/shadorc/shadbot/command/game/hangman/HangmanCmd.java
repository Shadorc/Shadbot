package me.shadorc.shadbot.command.game.hangman;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class HangmanCmd extends GameCmd<HangmanGame> {

    protected enum Difficulty {
        EASY, HARD;
    }

    protected static final int MIN_WORD_LENGTH = 5;
    protected static final int MAX_WORD_LENGTH = 10;

    private final WordsList easyWords;
    private final WordsList hardWords;

    public HangmanCmd() {
        super(List.of("hangman"));

        this.easyWords = new WordsList("https://gist.githubusercontent.com/deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt");
        this.hardWords = new WordsList("https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final Difficulty difficulty = Utils.parseEnum(Difficulty.class, context.getArg().orElse("easy"),
                new CommandException(String.format("`%s` is not a valid difficulty. %s",
                        context.getArg().orElse(""), FormatUtils.options(Difficulty.class))));

        if (difficulty == Difficulty.EASY && !this.easyWords.isLoaded()) {
            this.easyWords.load();
        } else if (difficulty == Difficulty.HARD && !this.hardWords.isLoaded()) {
            this.hardWords.load();
        }

        final HangmanGame hangmanManager = this.getManagers().putIfAbsent(context.getChannelId(), new HangmanGame(this, context, difficulty));
        if (hangmanManager == null) {
            final HangmanGame newHangmanManager = this.getManagers().get(context.getChannelId());
            newHangmanManager.start();
            return newHangmanManager.show();
        } else {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) A Hangman game has already been started by **%s**."
                                    + " Please, wait for him to finish.",
                            context.getUsername(), hangmanManager.getContext().getUsername()), channel))
                    .then();
        }
    }

    protected String getWord(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return this.easyWords.getRandomWord();
            case HARD:
                return this.hardWords.getRandomWord();
            default:
                throw new RuntimeException(String.format("Unknown difficulty: %s", difficulty.toString()));
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Start a Hangman game.")
                .addArg("difficulty", String.format("%s. The difficulty of the word to find", FormatUtils.format(Difficulty.class, "/")), true)
                .addField("Gains", String.format("The winner gets **%d coins** plus a bonus (**%d coins max.**) depending on his number of errors.",
                        HangmanGame.MIN_GAINS, HangmanGame.MAX_BONUS), false)
                .build();
    }
}
