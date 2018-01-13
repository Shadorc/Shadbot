package me.shadorc.shadbot.command.game.hangman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.json.JSONException;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.GAME, names = { "hangman" })
public class HangmanCmd extends AbstractCommand {

	private static final int MIN_WORD_LENGTH = 5;
	private static final int MAX_WORD_LENGTH = 10;

	protected static final ConcurrentHashMap<Long, HangmanManager> MANAGERS = new ConcurrentHashMap<>();
	protected static final List<String> WORDS = new ArrayList<>();

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(WORDS.isEmpty()) {
			LoadingMessage loadingMsg = new LoadingMessage("Loading word...", context.getChannel());
			loadingMsg.send();
			try {
				this.load();
			} catch (JSONException | IOException err) {
				Utils.handle("getting words list", context, err);
			}
			loadingMsg.delete();
		}

		HangmanManager hangmanManager = MANAGERS.get(context.getChannel().getLongID());

		if(hangmanManager == null) {
			try {
				hangmanManager = new HangmanManager(this, context.getChannel(), context.getAuthor());
				if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), hangmanManager) == null) {
					hangmanManager.start();
				}
			} catch (IOException err) {
				Utils.handle("getting a word", context, err);
			}
		} else {
			BotUtils.sendMessage(String.format(Emoji.INFO + " A Hangman game has already been started by **%s**. Please, wait for him to finish.",
					hangmanManager.getAuthor().getName()), context.getChannel());
		}
	}

	private void load() throws JSONException, IOException {
		String url = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";
		WORDS.addAll(StringUtils.split(NetUtils.getBody(url), "\n").stream()
				.filter(word -> MathUtils.isInRange(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
				.collect(Collectors.toList()));
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Start a Hangman game.")
				.setGains("The winner gets **%d coins** plus a bonus depending on the number of errors.", HangmanManager.MIN_GAINS)
				.build();
	}
}
