package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.api.trivia.TriviaResponse;
import me.shadorc.shadbot.api.trivia.TriviaResult;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class TriviaManager extends AbstractGameManager implements MessageInterceptor {

	protected static final int MIN_GAINS = 100;
	protected static final int MAX_BONUS = 100;
	protected static final int LIMITED_TIME = 30;

	private final Integer categoryId;
	private final ConcurrentHashMap<Snowflake, Boolean> alreadyAnswered;

	private long startTime;
	private String correctAnswer;
	private List<String> answers;

	public TriviaManager(Context context, Integer categoryId) {
		super(context);
		this.categoryId = categoryId;
		this.alreadyAnswered = new ConcurrentHashMap<>();
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	@Override
	public Mono<Void> start() {
		try {
			final URL url = new URL(String.format("https://opentdb.com/api.php?amount=1&category=%s", Objects.toString(categoryId, "")));
			TriviaResponse trivia = Utils.MAPPER.readValue(url, TriviaResponse.class);
			TriviaResult result = trivia.getResults().get(0);

			this.correctAnswer = result.getCorrectAnswer();

			if("multiple".equals(result.getType())) {
				this.answers = result.getIncorrectAnswers();
				this.answers.add(correctAnswer);
				Collections.shuffle(answers);
			} else {
				this.answers = List.of("True", "False");
			}

			final String description = String.format("**%s**%n%s",
					result.getQuestion(),
					FormatUtils.numberedList(answers.size(), answers.size(),
							count -> String.format("\t**%d**. %s", count, answers.get(count - 1))));

			MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);

			this.startTime = System.currentTimeMillis();
			this.schedule(() -> this.stop()
					.then(BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.", correctAnswer),
							this.getContext().getChannel()))
					.subscribe(), LIMITED_TIME, TimeUnit.SECONDS);

			return this.getContext().getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Trivia", null, avatarUrl)
							.setDescription(description)
							.addField("Category", String.format("`%s`", result.getCategory()), true)
							.addField("Type", String.format("`%s`", result.getType()), true)
							.addField("Difficulty", String.format("`%s`", result.getDifficulty()), true)
							.setFooter(String.format("You have %d seconds to answer.", LIMITED_TIME), null))
					.flatMap(embed -> BotUtils.sendMessage(embed, this.getContext().getChannel()))
					.then();

		} catch (IOException err) {
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Mono<Void> stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		TriviaCmd.MANAGERS.remove(this.getContext().getChannelId());
		return Mono.empty();
	}

	private Mono<Message> win(Member member) {
		final float coinsPerSec = (float) MAX_BONUS / LIMITED_TIME;
		final long remainingSec = LIMITED_TIME - TimeUnit.MILLISECONDS.toSeconds(TimeUtils.getMillisUntil(startTime));
		final int gains = MIN_GAINS + (int) Math.ceil(remainingSec * coinsPerSec);

		DatabaseManager.getDBMember(member.getGuildId(), member.getId()).addCoins(gains);
		MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);

		return this.stop()
				.then(BotUtils.sendMessage(String.format(Emoji.CLAP + " (**%s**) Correct ! You won **%d coins**.",
						member.getUsername(), gains), this.getContext().getChannel()));
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final Member member = event.getMember().get();
		return this.processIfNotCancelled(event.getMessage(), Mono.just(event.getMessage().getContent().get())
				.flatMap(content -> {
					// It's a number or a text
					Integer choice = NumberUtils.asIntBetween(content, 1, answers.size());

					// Message is a text and doesn't match any answers, ignore it
					if(choice == null && !answers.stream().anyMatch(content::equalsIgnoreCase)) {
						return Mono.just(false);
					}

					// If the user has already answered and has been warned, ignore him
					if(alreadyAnswered.containsKey(member.getId()) && alreadyAnswered.get(member.getId())) {
						return Mono.just(false);
					}

					final String answer = choice == null ? content : answers.get(choice - 1);

					Mono<Message> monoMessage;
					if(alreadyAnswered.containsKey(member.getId())) {
						monoMessage = BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You can only answer once.",
								member.getUsername()), event.getMessage().getChannel());
						alreadyAnswered.put(member.getId(), true);
					} else if(answer.equalsIgnoreCase(correctAnswer)) {
						monoMessage = this.win(member);

					} else {
						monoMessage = BotUtils.sendMessage(String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.",
								member.getUsername()), event.getMessage().getChannel());
						alreadyAnswered.put(member.getId(), false);
					}

					return monoMessage.thenReturn(true);
				}));
	}

}
