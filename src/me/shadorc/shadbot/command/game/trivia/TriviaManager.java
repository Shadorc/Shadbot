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
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class TriviaManager extends GameManager {

	protected static final int MIN_GAINS = 100;
	private static final int MAX_BONUS = 100;

	private final TriviaResult trivia;
	private final Map<Snowflake, TriviaPlayer> players;
	private final List<String> answers;

	private long startTime;

	// Trivia API doc : https://opentdb.com/api_config.php
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

		this.players = new ConcurrentHashMap<>();
	}

	@Override
	public void start() {
		this.schedule(this.end());
		this.startTime = System.currentTimeMillis();
		new TriviaInputs(this.getContext().getClient(), this).subscribe();
	}

	@Override
	public Mono<Void> end() {
		return this.getContext().getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.",
						this.trivia.getCorrectAnswer()), channel))
				.then(Mono.fromRunnable(this::stop));
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

	protected Mono<Message> win(Member member) {
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

	public void hasAnswered(Snowflake userId) {
		if(this.players.containsKey(userId)) {
			this.players.get(userId).setAnswered(true);
		} else {
			this.players.put(userId, new TriviaPlayer(userId));
		}
	}

	public Map<Snowflake, TriviaPlayer> getPlayers() {
		return Collections.unmodifiableMap(this.players);
	}

	public List<String> getAnswers() {
		return Collections.unmodifiableList(this.answers);
	}

	public String getCorrectAnswer() {
		return this.trivia.getCorrectAnswer();
	}

}
