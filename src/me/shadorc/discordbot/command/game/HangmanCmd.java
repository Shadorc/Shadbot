package me.shadorc.discordbot.command.game;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class HangmanCmd extends AbstractCommand {

	protected static final List<String> IMG_LIST = Arrays.asList(
			"https://upload.wikimedia.org/wikipedia/commons/8/8b/Hangman-0.png",
			"https://upload.wikimedia.org/wikipedia/commons/3/30/Hangman-1.png",
			"https://upload.wikimedia.org/wikipedia/commons/7/70/Hangman-2.png",
			"https://upload.wikimedia.org/wikipedia/commons/9/97/Hangman-3.png",
			"https://upload.wikimedia.org/wikipedia/commons/2/27/Hangman-4.png",
			"https://upload.wikimedia.org/wikipedia/commons/6/6b/Hangman-5.png",
			"https://upload.wikimedia.org/wikipedia/commons/d/d6/Hangman-6.png");

	protected static final ConcurrentHashMap<Long, HangmanManager> CHANNELS_HANGMAN = new ConcurrentHashMap<>();

	private final RateLimiter rateLimiter;

	public HangmanCmd() {
		super(CommandCategory.GAME, Role.USER, "hangman");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		HangmanManager hangmanManager = CHANNELS_HANGMAN.get(context.getChannel().getLongID());

		if(hangmanManager == null) {
			try {
				hangmanManager = new HangmanManager(context);
				hangmanManager.start();
				CHANNELS_HANGMAN.putIfAbsent(context.getChannel().getLongID(), hangmanManager);

			} catch (IOException err) {
				ExceptionUtils.manageException("getting a word", context, err);
			}

		} else {
			BotUtils.sendMessage(Emoji.INFO + " A Hangman game has already been started by **"
					+ hangmanManager.getAuthor().getName() + "**. Please, wait for him to finish.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a Hangman game.**")
				.appendField("Gains", "The winner gets **" + HangmanManager.MIN_GAINS + " coins** plus a bonus depending on the number of errors.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	protected class HangmanManager implements MessageListener {

		protected static final int MIN_GAINS = 150;
		private static final int MAX_BONUS = 100;
		private static final int MAX_WORD_LENGTH = 10;
		private static final int MIN_WORD_LENGTH = 5;
		private static final int MAX_TRY = 7;

		private final Context context;
		private final String word;
		private final List<String> charsTested;

		private IMessage message;
		private Timer idleTimer;
		private int failsCount;

		protected HangmanManager(Context context) throws IOException {
			this.context = context;
			this.word = this.getWord();
			this.charsTested = new ArrayList<>();
			this.failsCount = 0;
		}

		private String getWord() throws IOException {
			String word;
			do {
				word = NetUtils.getBody("http://setgetgo.com/randomword/get.php");
			} while(word.length() <= MIN_WORD_LENGTH || word.length() >= MAX_WORD_LENGTH || StringUtils.capitalize(word).equals(word));
			return word;
		}

		protected void start() {
			idleTimer = new Timer((int) TimeUnit.MINUTES.toMillis(1), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					HangmanManager.this.stop();
				}
			});
			idleTimer.start();
			MessageManager.addListener(context.getChannel(), this);
			this.show();
		}

		protected void stop() {
			MessageManager.removeListener(context.getChannel());
			idleTimer.stop();
			CHANNELS_HANGMAN.remove(context.getChannel().getLongID());
		}

		private void showResultAndStop(boolean win) {
			this.show();
			if(win) {
				int gains = (int) Math.ceil(MIN_GAINS + ((float) MAX_BONUS / MAX_TRY) * ((float) MAX_TRY - failsCount));
				BotUtils.sendMessage(Emoji.PURSE + " Well played **" + context.getAuthorName() + "**, you found the word ! "
						+ "You won **" + StringUtils.pluralOf(gains, "coin") + "**.", context.getChannel());
				DatabaseManager.addCoins(context.getGuild(), context.getAuthor(), gains);
				StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, HangmanCmd.this.getFirstName(), gains);
			} else {
				BotUtils.sendMessage(Emoji.THUMBSDOWN + " You lose, the word to guess was **" + word + "** !", context.getChannel());
			}
			this.stop();
		}

		private void checkLetter(String chr) {
			idleTimer.restart();

			if(!word.contains(chr)) {
				failsCount++;
				if(failsCount >= MAX_TRY) {
					this.showResultAndStop(false);
					return;
				}
			}

			if(!charsTested.contains(chr)) {
				charsTested.add(chr);
			}

			if(this.getRepresentation().replace(" ", "").replace("*", "").equalsIgnoreCase(word)) {
				this.showResultAndStop(true);
				return;
			}

			this.show();
		}

		private void checkWord(String word) {
			idleTimer.restart();

			if(!this.word.equals(word)) {
				failsCount++;
				if(failsCount >= MAX_TRY) {
					this.showResultAndStop(false);
					return;
				}
				this.show();
				return;
			}

			charsTested.addAll(Arrays.asList(word.split("")));
			this.showResultAndStop(true);
		}

		private String getRepresentation() {
			StringBuilder strBuilder = new StringBuilder();
			for(int i = 0; i < word.length(); i++) {
				if(i == 0 || i == word.length() - 1 || charsTested.contains(Character.toString(word.charAt(i)))) {
					strBuilder.append("**" + Character.toUpperCase(word.charAt(i)) + "** ");
				} else {
					strBuilder.append("\\_ ");
				}
			}
			return strBuilder.toString();
		}

		private void show() {
			if(message != null && BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_MESSAGES)) {
				message.delete();
			}

			EmbedBuilder builder = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorIcon(context.getAuthor().getAvatarURL())
					.withAuthorName("Hangman Game")
					.withThumbnail("https://lh5.ggpht.com/nIoJylIWCj1gKv9dxtd4CFE2aeXvG7MbvP0BNFTtTFusYlxozJRQmHizsIDxydaa7DHT=w300")
					.withDescription("Type letters or enter a word if you think you've guessed it."
							+ "\nUse `" + context.getPrefix() + "cancel` to cancel this game.")
					.appendField("Word", this.getRepresentation(), false)
					.appendField("Letters tested", StringUtils.formatList(charsTested, chr -> chr.toString().toUpperCase(), ", "), false);

			if(idleTimer.isRunning()) {
				builder.withFooterText("This game will be cancelled in " + TimeUnit.MILLISECONDS.toMinutes(idleTimer.getDelay())
						+ "min in case of inactivity.");
			} else {
				builder.withFooterText("Finished.");
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
			if(message.getAuthor().equals(context.getAuthor())) {
				String content = message.getContent();

				String prefix = (String) DatabaseManager.getSetting(message.getGuild(), Setting.PREFIX);
				if(content.equalsIgnoreCase(prefix + "cancel")) {
					BotUtils.sendMessage(Emoji.CHECK_MARK + " Game canceled.", message.getChannel());
					this.stop();
					return true;
				}

				// Check only if content is an unique word
				if(content.matches("[a-zA-Z]+")) {
					if(content.length() == 1) {
						this.checkLetter(content.toLowerCase());
					} else if(content.length() >= MIN_WORD_LENGTH) {
						this.checkWord(content);
					}
				}

			}
			return false;
		}
	}
}
