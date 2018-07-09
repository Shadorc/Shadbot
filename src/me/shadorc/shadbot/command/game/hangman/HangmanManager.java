package me.shadorc.shadbot.command.game.hangman;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.UpdateableMessage;

public class HangmanManager extends AbstractGameManager implements MessageInterceptor {

	private static final List<String> IMG_LIST = List.of(
			"https://upload.wikimedia.org/wikipedia/commons/8/8b/Hangman-0.png",
			"https://upload.wikimedia.org/wikipedia/commons/3/30/Hangman-1.png",
			"https://upload.wikimedia.org/wikipedia/commons/7/70/Hangman-2.png",
			"https://upload.wikimedia.org/wikipedia/commons/9/97/Hangman-3.png",
			"https://upload.wikimedia.org/wikipedia/commons/2/27/Hangman-4.png",
			"https://upload.wikimedia.org/wikipedia/commons/6/6b/Hangman-5.png",
			"https://upload.wikimedia.org/wikipedia/commons/d/d6/Hangman-6.png");

	protected static final int MIN_GAINS = 200;
	private static final int MAX_BONUS = 200;
	private static final int IDLE_MIN = 1;

	private final RateLimiter rateLimiter;
	private final UpdateableMessage message;
	private final String word;
	private final List<String> charsTested;

	private int failsCount;

	public HangmanManager(AbstractCommand cmd, String prefix, IChannel channel, IUser author, Difficulty difficulty) {
		super(cmd, prefix, channel, author);
		this.rateLimiter = new RateLimiter(3, 2, ChronoUnit.SECONDS);
		this.message = new UpdateableMessage(channel);
		this.word = HangmanCmd.getWord(difficulty);
		this.charsTested = new ArrayList<>();
		this.failsCount = 0;
	}

	@Override
	public void start() {
		MessageInterceptorManager.addInterceptor(this.getMessageChannel(), this);
		this.show();
		this.schedule(() -> this.stop(), IDLE_MIN, TimeUnit.MINUTES);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getMessageChannel(), this);
		HangmanCmd.MANAGERS.remove(this.getMessageChannel().getLongID());
	}

	private void showResultAndStop(boolean win) {
		this.show();
		if(win) {
			int gains = (int) Math.ceil(MIN_GAINS + ((float) MAX_BONUS / IMG_LIST.size()) * ((float) IMG_LIST.size() - failsCount));
			BotUtils.sendMessage(String.format(Emoji.PURSE + " Well played **%s**, you found the word ! You won **%s**.",
					this.getAuthor().getName(), FormatUtils.formatCoins(gains)), this.getMessageChannel());
			Database.getDBUser(this.getGuild(), this.getAuthor()).addCoins(gains);
			MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);
		} else {
			BotUtils.sendMessage(String.format(Emoji.THUMBSDOWN + " You lose, the word to guess was **%s** !", word), this.getMessageChannel());
		}
		this.stop();
	}

	private void checkLetter(String chr) {
		this.schedule(() -> this.stop(), IDLE_MIN, TimeUnit.MINUTES);

		if(charsTested.contains(chr)) {
			return;
		}

		if(!word.contains(chr)) {
			failsCount++;
			if(failsCount == IMG_LIST.size()) {
				this.showResultAndStop(false);
				return;
			}
		}

		charsTested.add(chr);

		if(StringUtils.remove(this.getRepresentation(word), "\\", " ", "*").equalsIgnoreCase(word)) {
			this.showResultAndStop(true);
			return;
		}

		this.show();
	}

	private void checkWord(String word) {
		this.schedule(() -> this.stop(), IDLE_MIN, TimeUnit.MINUTES);

		if(!this.word.equalsIgnoreCase(word)) {
			failsCount++;
			if(failsCount == IMG_LIST.size()) {
				this.showResultAndStop(false);
				return;
			}
			this.show();
			return;
		}

		charsTested.addAll(StringUtils.split(word, ""));
		this.showResultAndStop(true);
	}

	private String getRepresentation(String word) {
		return String.format("**%s**",
				FormatUtils.format(StringUtils.split(word, ""), letter -> charsTested.contains(letter) ? letter.toUpperCase() : "\\_", " "));
	}

	private void show() {
		List<String> missesList = charsTested.stream().filter(letter -> !word.contains(letter)).collect(Collectors.toList());
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorIcon(this.getAuthor().getAvatarURL())
				.withAuthorName("Hangman Game")
				.withThumbnail("https://lh5.ggpht.com/nIoJylIWCj1gKv9dxtd4CFE2aeXvG7MbvP0BNFTtTFusYlxozJRQmHizsIDxydaa7DHT=w300")
				.withDescription("Type letters or enter a word if you think you've guessed it.")
				.addField("Word", this.getRepresentation(word), false)
				.addField("Misses", FormatUtils.format(missesList, chr -> chr.toString().toUpperCase(), ", "), false);

		if(this.isTaskDone()) {
			embed.withFooterText("Finished.");
		} else {
			embed.withFooterText(String.format("Use %scancel to cancel this game (Automatically cancelled in %d min in case of inactivity)",
					this.getPrefix(), IDLE_MIN));
		}

		if(failsCount > 0) {
			embed.withImage(IMG_LIST.get(Math.min(IMG_LIST.size(), failsCount) - 1));
		}

		RequestFuture<IMessage> msgRequest = message.send(embed.build());
		if(msgRequest != null) {
			msgRequest.get();
		}
	}

	@Override
	public boolean intercept(IMessage message) {
		if(this.isCancelCmd(message)) {
			return true;
		}

		if(!message.getAuthor().equals(this.getAuthor())) {
			return false;
		}

		String content = message.getContent().toLowerCase().trim();

		// Check only if content is an unique word/letter
		if(!content.matches("[a-z]+")) {
			return false;
		}

		if(content.length() == 1 && !rateLimiter.isLimited(this.getMessageChannel(), message.getAuthor())) {
			this.checkLetter(content);
		} else if(content.length() == word.length() && !rateLimiter.isLimited(this.getMessageChannel(), message.getAuthor())) {
			this.checkWord(content);
		}

		return false;
	}

}