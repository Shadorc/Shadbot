package me.shadorc.shadbot.command.game.hangman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAME, names = { "hangman" })
public class HangmanCmd extends AbstractCommand {

	protected enum Difficulty {
		EASY, HARD;
	}

	private static final String HARD_WORDS_URL = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";
	private static final String EASY_WORDS_URL = "https://gist.githubusercontent.com/deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt";

	private static final int MIN_WORD_LENGTH = 5;
	private static final int MAX_WORD_LENGTH = 10;

	protected static final ConcurrentHashMap<Snowflake, HangmanManager> MANAGERS = new ConcurrentHashMap<>();
	protected static final List<String> HARD_WORDS = new ArrayList<>();
	protected static final List<String> EASY_WORDS = new ArrayList<>();

	@Override
	public Mono<Void> execute(Context context) {
		final Difficulty difficulty = Utils.getEnum(Difficulty.class, context.getArg().orElse("easy"));

		if(difficulty == null) {
			throw new CommandException(String.format("`%s` is not a valid difficulty. %s",
					context.getArg().get(), FormatUtils.options(Difficulty.class)));
		}

		if(HARD_WORDS.isEmpty() || EASY_WORDS.isEmpty()) {
			final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
			try {
				this.load();
			} catch (final IOException err) {
				throw Exceptions.propagate(err);
			}
			loadingMsg.stopTyping();
		}

		final HangmanManager hangmanManager = MANAGERS.putIfAbsent(context.getChannelId(), new HangmanManager(context, difficulty));
		if(hangmanManager == null) {
			final HangmanManager newHangmanManager = MANAGERS.get(context.getChannelId());
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

	private void load() throws IOException {
		if(HARD_WORDS.isEmpty()) {
			HARD_WORDS.addAll(
					StringUtils.split(NetUtils.getBody(HARD_WORDS_URL), "\n").stream()
							.filter(word -> NumberUtils.isInRange(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
							.limit(500)
							.collect(Collectors.toList()));
		}

		if(EASY_WORDS.isEmpty()) {
			EASY_WORDS.addAll(StringUtils.split(NetUtils.getBody(EASY_WORDS_URL), "\n").stream()
					.filter(word -> NumberUtils.isInRange(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
					.limit(500)
					.collect(Collectors.toList()));
		}
	}

	protected static String getWord(Difficulty difficulty) {
		return difficulty.equals(Difficulty.EASY) ? Utils.randValue(EASY_WORDS) : Utils.randValue(HARD_WORDS);
	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start a Hangman game.")
				.addArg("difficulty", String.format("%s. The difficulty of the word to find", FormatUtils.format(Difficulty.class, "/")), true)
				.setGains("The winner gets **%d coins** plus a bonus depending on the number of errors.", HangmanManager.MIN_GAINS)
				.build();
	}
}
