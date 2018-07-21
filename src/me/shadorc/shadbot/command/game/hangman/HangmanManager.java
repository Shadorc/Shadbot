package me.shadorc.shadbot.command.game.hangman;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.command.game.hangman.HangmanCmd.Difficulty;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.DatabaseManager;
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
import reactor.core.publisher.Mono;

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
	protected static final int MAX_BONUS = 200;
	protected static final int IDLE_MIN = 1;

	private final RateLimiter rateLimiter;
	private final UpdateableMessage updateableMessage;
	private final String word;
	private final List<String> lettersTested;

	private int failsCount;

	public HangmanManager(Context context, Difficulty difficulty) {
		super(context);
		this.rateLimiter = new RateLimiter(3, 2, ChronoUnit.SECONDS);
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
		this.word = HangmanCmd.getWord(difficulty);
		this.lettersTested = new ArrayList<>();
		this.failsCount = 0;
	}

	@Override
	public Mono<Void> start() {
		this.schedule(() -> this.stop().subscribe(), IDLE_MIN, TimeUnit.MINUTES);
		MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
		return this.show().then();
	}

	@Override
	public Mono<Void> stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		HangmanCmd.MANAGERS.remove(this.getContext().getChannelId());
		return Mono.empty();
	}

	private Mono<Void> show() {
		List<String> missedLetters = lettersTested.stream()
				.filter(letter -> !word.contains(letter))
				.map(String::toUpperCase)
				.collect(Collectors.toList());

		return this.getContext().getAvatarUrl()
				.map(avatarUrl -> {
					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Hangman Game", null, avatarUrl)
							.setThumbnail("https://lh5.ggpht.com/nIoJylIWCj1gKv9dxtd4CFE2aeXvG7MbvP0BNFTtTFusYlxozJRQmHizsIDxydaa7DHT=w300")
							.setDescription("Type letters or enter a word if you think you've guessed it.")
							.addField("Word", this.getRepresentation(word), false);

					if(!missedLetters.isEmpty()) {
						embed.addField("Misses", FormatUtils.format(missedLetters, Object::toString, ", "), false);
					}

					if(this.isTaskDone()) {
						embed.setFooter("Finished.", null);
					} else {
						embed.setFooter(String.format("Use %scancel to cancel this game (Automatically cancelled in %d min in case of inactivity)",
								this.getContext().getPrefix(), IDLE_MIN), null);
					}

					if(failsCount > 0) {
						embed.setImage(IMG_LIST.get(Math.min(IMG_LIST.size(), failsCount) - 1));
					}

					return embed;
				})
				.flatMap(embed -> updateableMessage.send(embed))
				.then();
	}

	private Mono<Void> showResultAndStop(boolean win) {
		String text;
		if(win) {
			final float bonusPerImg = (float) MAX_BONUS / IMG_LIST.size();
			final float imagesRemaining = IMG_LIST.size() - failsCount;
			int gains = (int) Math.ceil(MIN_GAINS + bonusPerImg * imagesRemaining);

			DatabaseManager.getDBMember(this.getContext().getGuildId(), this.getContext().getAuthorId()).addCoins(gains);
			MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);

			text = String.format(Emoji.PURSE + " (**%s**) Well played, you found the word ! You won **%s**.",
					this.getContext().getUsername(), FormatUtils.formatCoins(gains));
		} else {
			text = String.format(Emoji.THUMBSDOWN + " (**%s**) You lose, the word to guess was **%s** !",
					this.getContext().getUsername(), word);
		}

		return this.show()
				.then(BotUtils.sendMessage(text, this.getContext().getChannel()))
				.then(this.stop());
	}

	private Mono<Void> checkLetter(String chr) {
		// Reset IDLE timer
		this.schedule(() -> this.stop().subscribe(), IDLE_MIN, TimeUnit.MINUTES);

		if(lettersTested.contains(chr)) {
			return Mono.empty();
		}

		if(!word.contains(chr)) {
			failsCount++;
			if(failsCount == IMG_LIST.size()) {
				return this.showResultAndStop(false);
			}
		}

		lettersTested.add(chr);

		// The word has been entirely guessed
		if(StringUtils.remove(this.getRepresentation(word), "\\", " ", "*").equalsIgnoreCase(word)) {
			return this.showResultAndStop(true);
		}

		return this.show();
	}

	private Mono<Void> checkWord(String word) {
		// Reset IDLE timer
		this.schedule(() -> this.stop().subscribe(), IDLE_MIN, TimeUnit.MINUTES);

		// If the word has been guessed
		if(this.word.equalsIgnoreCase(word)) {
			lettersTested.addAll(StringUtils.split(word, ""));
			return this.showResultAndStop(true);
		}

		failsCount++;
		if(failsCount == IMG_LIST.size()) {
			return this.showResultAndStop(false);
		}
		return this.show();
	}

	private String getRepresentation(String word) {
		return String.format("**%s**",
				FormatUtils.format(StringUtils.split(word, ""),
						letter -> lettersTested.contains(letter) ? letter.toUpperCase() : "\\_", " "));
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		return this.processIfNotCancelled(event.getMessage(), Mono.just(event.getMessage().getAuthorId().get())
				.flatMap(authorId -> {
					final Context context = this.getContext();

					if(!authorId.equals(context.getAuthorId())) {
						return Mono.just(false);
					}

					final String content = event.getMessage().getContent().get().toLowerCase().trim();

					// Check only if content is an unique word/letter
					if(!content.matches("[a-z]+")) {
						return Mono.just(false);
					}

					Mono<Void> checkMono = Mono.empty();
					if(content.length() == 1 && !rateLimiter.isLimitedAndWarn(
							context.getClient(), context.getGuildId(), context.getChannelId(), context.getAuthorId())) {
						checkMono = this.checkLetter(content);
					} else if(content.length() == word.length() && !rateLimiter.isLimitedAndWarn(
							context.getClient(), context.getGuildId(), context.getChannelId(), context.getAuthorId())) {
						checkMono = this.checkWord(content);
					}

					return checkMono.thenReturn(false);
				}));
	}

}