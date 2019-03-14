package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.api.trivia.TriviaResponse;
import me.shadorc.shadbot.api.trivia.TriviaResult;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.core.game.GameManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class TriviaManager extends GameManager implements MessageInterceptor {

	protected static final int MIN_GAINS = 100;
	private static final int MAX_BONUS = 100;

	private final TriviaResult trivia;
	private final Map<Snowflake, Boolean> alreadyAnswered;
	private final List<String> answers;

	private long startTime;

	public TriviaManager(GameCmd<TriviaManager> gameCmd, Context context, Integer categoryId) {
		super(gameCmd, context, Duration.ofSeconds(30));

		try {
			final String url = String.format("https://opentdb.com/api.php?amount=1&category=%s", Objects.toString(categoryId, ""));
			final TriviaResponse response = Utils.MAPPER.readValue(NetUtils.getJSON(url), TriviaResponse.class);
			this.trivia = response.getResults().get(0);
		} catch (final IOException err) {
			throw Exceptions.propagate(err);
		}

		this.answers = new ArrayList<>();
		if(this.trivia.getType().equals("multiple")) {
			this.answers.addAll(this.trivia.getIncorrectAnswers());
			this.answers.add(this.trivia.getCorrectAnswer());
			Collections.shuffle(this.answers);
		} else {
			this.answers.addAll(List.of("True", "False"));
		}

		this.alreadyAnswered = new ConcurrentHashMap<>();
	}

	// Trivia API doc : https://opentdb.com/api_config.php
	@Override
	public void start() {
		MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
		this.schedule(Mono.fromRunnable(this::stop)
				.then(this.getContext().getChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.",
						this.trivia.getCorrectAnswer()), channel)));
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public Mono<Void> show() {
		final String description = String.format("**%s**%n%s",
				this.trivia.getQuestion(),
				FormatUtils.numberedList(this.answers.size(), this.answers.size(),
						count -> String.format("\t**%d**. %s", count, this.answers.get(count - 1))));

		final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.setAuthor("Trivia", null, this.getContext().getAvatarUrl())
						.setDescription(description)
						.addField("Category", String.format("`%s`", this.trivia.getCategory()), true)
						.addField("Type", String.format("`%s`", this.trivia.getType()), true)
						.addField("Difficulty", String.format("`%s`", this.trivia.getDifficulty()), true)
						.setFooter(String.format("You have %d seconds to answer.", this.getDuration().toSeconds()), null));

		return this.getContext().getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
				.then();
	}

	private Mono<Message> win(Member member) {
		final float coinsPerSec = (float) MAX_BONUS / this.getDuration().toSeconds();
		final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
		final int gains = (int) Math.ceil(MIN_GAINS + remainingDuration.toSeconds() * coinsPerSec);

		Shadbot.getDatabase().getDBMember(member.getGuildId(), member.getId()).addCoins(gains);
		StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), gains);

		this.stop();
		return this.getContext().getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CLAP + " (**%s**) Correct ! You won **%d coins**.",
						member.getUsername(), gains), channel));
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final Member member = event.getMember().get();
		return this.cancelOrDo(event.getMessage(), Mono.justOrEmpty(event.getMessage().getContent())
				.flatMap(content -> {
					// It's a number or a text
					final Integer choice = NumberUtils.asIntBetween(content, 1, this.answers.size());

					// Message is a text and doesn't match any answers, ignore it
					if(choice == null && !this.answers.stream().anyMatch(content::equalsIgnoreCase)) {
						return Mono.just(false);
					}

					// If the user has already answered and has been warned, ignore him
					if(this.alreadyAnswered.getOrDefault(member.getId(), false)) {
						return Mono.just(false);
					}

					final String answer = choice == null ? content : this.answers.get(choice - 1);

					if(this.alreadyAnswered.containsKey(member.getId())) {
						this.alreadyAnswered.put(member.getId(), true);
						return event.getMessage().getChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You can only answer once.",
										member.getUsername()), channel))
								.thenReturn(true);
					} else if(answer.equalsIgnoreCase(this.trivia.getCorrectAnswer())) {
						return this.win(member).thenReturn(true);
					} else {
						this.alreadyAnswered.put(member.getId(), false);
						return event.getMessage().getChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.",
										member.getUsername()), channel))
								.thenReturn(true);
					}
				}));
	}

}
