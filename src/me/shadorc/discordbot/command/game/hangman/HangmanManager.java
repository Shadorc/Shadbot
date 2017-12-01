package me.shadorc.discordbot.command.game.hangman;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

class HangmanManager implements MessageListener {

	protected static final ConcurrentHashMap<Long, HangmanManager> CHANNELS_HANGMAN = new ConcurrentHashMap<>();

	private static final List<String> IMG_LIST = Arrays.asList(
			"https://upload.wikimedia.org/wikipedia/commons/8/8b/Hangman-0.png",
			"https://upload.wikimedia.org/wikipedia/commons/3/30/Hangman-1.png",
			"https://upload.wikimedia.org/wikipedia/commons/7/70/Hangman-2.png",
			"https://upload.wikimedia.org/wikipedia/commons/9/97/Hangman-3.png",
			"https://upload.wikimedia.org/wikipedia/commons/2/27/Hangman-4.png",
			"https://upload.wikimedia.org/wikipedia/commons/6/6b/Hangman-5.png",
			"https://upload.wikimedia.org/wikipedia/commons/d/d6/Hangman-6.png");

	protected static final int MIN_WORD_LENGTH = 5;
	protected static final int MAX_WORD_LENGTH = 10;
	protected static final int MIN_GAINS = 200;
	private static final int MAX_BONUS = 200;
	private static final int IDLE_MIN = 1;

	private final RateLimiter rateLimiter;
	private final Context context;
	private final String word;
	private final List<String> charsTested;
	private final ScheduledExecutorService executor;

	private ScheduledFuture<?> leaveTask;
	private IMessage message;
	private int failsCount;

	protected HangmanManager(Context context) throws IOException {
		this.rateLimiter = new RateLimiter(RateLimiter.DEFAULT_COOLDOWN, ChronoUnit.SECONDS);
		this.context = context;
		this.word = HangmanUtils.getWord();
		this.charsTested = new ArrayList<>();
		this.failsCount = 0;
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	protected void start() {
		MessageManager.addListener(context.getChannel(), this);
		this.restartIdleTimer();
		this.show();
	}

	protected void stop() {
		leaveTask.cancel(false);
		executor.shutdownNow();

		MessageManager.removeListener(context.getChannel(), this);
		CHANNELS_HANGMAN.remove(context.getChannel().getLongID());
	}

	private void restartIdleTimer() {
		if(leaveTask != null) {
			leaveTask.cancel(false);
		}
		leaveTask = executor.schedule(() -> this.stop(), IDLE_MIN, TimeUnit.MINUTES);
	}

	private void showResultAndStop(boolean win) {
		this.show();
		if(win) {
			int gains = (int) Math.ceil(MIN_GAINS + ((float) MAX_BONUS / IMG_LIST.size()) * ((float) IMG_LIST.size() - failsCount));
			BotUtils.sendMessage(Emoji.PURSE + " Well played **" + context.getAuthorName() + "**, you found the word ! "
					+ "You won **" + FormatUtils.formatCoins(gains) + "**.", context.getChannel());
			DatabaseManager.addCoins(context.getChannel(), context.getAuthor(), gains);
			StatsManager.increment(CommandManager.getFirstName(context.getCommand()), gains);
		} else {
			BotUtils.sendMessage(Emoji.THUMBSDOWN + " You lose, the word to guess was **" + word + "** !", context.getChannel());
		}
		this.stop();
	}

	private void checkLetter(String chr) {
		this.restartIdleTimer();

		if(!word.contains(chr)) {
			failsCount++;
			if(failsCount == IMG_LIST.size()) {
				this.showResultAndStop(false);
				return;
			}
		}

		if(!charsTested.contains(chr)) {
			charsTested.add(chr);
		}

		if(HangmanUtils.getRepresentation(word, charsTested).replace(" ", "").replace("*", "").equalsIgnoreCase(word)) {
			this.showResultAndStop(true);
			return;
		}

		this.show();
	}

	private void checkWord(String word) {
		this.restartIdleTimer();

		if(!this.word.equals(word)) {
			failsCount++;
			if(failsCount == IMG_LIST.size()) {
				this.showResultAndStop(false);
				return;
			}
			this.show();
			return;
		}

		charsTested.addAll(Arrays.asList(word.split("")));
		this.showResultAndStop(true);
	}

	private void show() {
		BotUtils.deleteIfPossible(context.getChannel(), message);

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorIcon(context.getAuthor().getAvatarURL())
				.withAuthorName("Hangman Game")
				.withThumbnail("https://lh5.ggpht.com/nIoJylIWCj1gKv9dxtd4CFE2aeXvG7MbvP0BNFTtTFusYlxozJRQmHizsIDxydaa7DHT=w300")
				.withDescription("Type letters or enter a word if you think you've guessed it."
						+ "\nUse `" + context.getPrefix() + "cancel` to cancel this game.")
				.appendField("Word", HangmanUtils.getRepresentation(word, charsTested), false)
				.appendField("Letters tested", FormatUtils.formatList(charsTested, chr -> chr.toString().toUpperCase(), ", "), false);

		if(leaveTask.isDone()) {
			builder.withFooterText("Finished.");
		} else {
			builder.withFooterText("This game will be cancelled in " + IDLE_MIN + "min in case of inactivity.");
		}

		if(failsCount > 0) {
			builder.withImage(IMG_LIST.get(failsCount - 1));
		}

		message = BotUtils.sendMessage(builder.build(), context.getChannel()).get();
	}

	public IUser getAuthor() {
		return context.getAuthor();
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		if(!message.getAuthor().equals(context.getAuthor())) {
			return false;
		}

		String content = message.getContent();

		String prefix = (String) DatabaseManager.getSetting(message.getGuild(), Setting.PREFIX);
		if(content.equalsIgnoreCase(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Game cancelled.", message.getChannel());
			this.stop();
			return true;
		}

		// Check only if content is an unique word/letter
		if(content.matches("[a-zA-Z]+")) {
			if(content.length() == 1 && !rateLimiter.isLimited(message.getGuild(), message.getAuthor())) {
				this.checkLetter(content.toLowerCase());
			} else if(content.length() == word.length() && !rateLimiter.isLimited(message.getGuild(), message.getAuthor())) {
				this.checkWord(content);
			}
		}

		return false;
	}
}