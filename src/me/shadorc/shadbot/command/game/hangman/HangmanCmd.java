
package me.shadorc.shadbot.command.game.hangman;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class HangmanCmd extends GameCmd<HangmanManager> {

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
		final Difficulty difficulty = Utils.getEnum(Difficulty.class, context.getArg().orElse("easy"));

		if(difficulty == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid difficulty. %s",
					context.getArg().get(), FormatUtils.options(Difficulty.class))));
		}

		try {
			if(difficulty.equals(Difficulty.EASY) && !this.easyWords.isLoaded()) {
				this.easyWords.load();
			} else if(difficulty.equals(Difficulty.HARD) && !this.hardWords.isLoaded()) {
				this.hardWords.load();
			}
		} catch (IOException err) {
			throw Exceptions.propagate(err);
		}

		final HangmanManager hangmanManager = this.getManagers().putIfAbsent(context.getChannelId(), new HangmanManager(this, context, difficulty));
		if(hangmanManager == null) {
			final HangmanManager newHangmanManager = this.getManagers().get(context.getChannelId());
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
				.setGains("The winner gets **%d coins** plus a bonus depending on the number of errors.", HangmanManager.MIN_GAINS)
				.build();
	}
}
