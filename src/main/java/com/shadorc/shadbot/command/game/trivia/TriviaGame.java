package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.api.json.trivia.TriviaResponse;
import com.shadorc.shadbot.api.json.trivia.TriviaResult;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.game.player.Player;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import io.prometheus.client.Summary;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TriviaGame extends MultiplayerGame<TriviaCmd, TriviaPlayer> {

    private static final Summary TRIVIA_SUMMARY = Summary.build()
            .name("game_trivia")
            .help("Trivia game")
            .labelNames("result")
            .register();

    @Nullable
    private final Integer categoryId;
    private final List<String> answers;

    private TriviaResult trivia;
    private long startTime;

    // Trivia API doc : https://opentdb.com/api_config.php
    public TriviaGame(TriviaCmd gameCmd, Context context, @Nullable Integer categoryId) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.categoryId = categoryId;
        this.answers = new ArrayList<>();
    }

    @Override
    public Mono<Void> start() {
        final String url = String.format("https://opentdb.com/api.php?amount=1&category=%s",
                Objects.toString(this.categoryId, ""));
        return NetUtils.get(url, TriviaResponse.class)
                .map(TriviaResponse::getResults)
                .map(list -> list.get(0))
                .doOnNext(trivia -> {
                    this.trivia = trivia;

                    if ("multiple".equals(this.trivia.getType())) {
                        this.answers.addAll(this.trivia.getIncorrectAnswers());
                        this.answers.add(this.trivia.getCorrectAnswer());
                        Collections.shuffle(this.answers);
                    } else {
                        this.answers.addAll(List.of("True", "False"));
                    }

                    this.schedule(this.end());
                    this.startTime = System.currentTimeMillis();
                    TriviaInputs.create(this.getContext().getClient(), this).listen();
                })
                .then();
    }

    @Override
    public Mono<Void> end() {
        return this.getContext().getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.HOURGLASS + " Time elapsed, the correct answer was **%s**.",
                                this.trivia.getCorrectAnswer()), channel))
                .then(Mono.fromRunnable(this::stop));
    }

    @Override
    public Mono<Void> show() {
        return Mono.defer(() -> {
            final String description = String.format("**%s**%n%s",
                    this.trivia.getQuestion(),
                    FormatUtils.numberedList(this.answers.size(), this.answers.size(),
                            count -> String.format("\t**%d**. %s", count, this.answers.get(count - 1))));

            final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor("Trivia", null, this.getContext().getAvatarUrl())
                            .setDescription(description)
                            .addField("Category", String.format("`%s`", this.trivia.getCategory()), true)
                            .addField("Type", String.format("`%s`", this.trivia.getType()), true)
                            .addField("Difficulty", String.format("`%s`", this.trivia.getDifficulty()), true)
                            .setFooter(String.format("You have %d seconds to answer. Use %scancel to force the stop.",
                                    this.getDuration().toSeconds(), this.getContext().getPrefix()), null));

            return this.getContext().getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                    .then();
        });
    }

    protected Mono<Message> win(Member member) {
        final double coinsPerSec = (double) Constants.MAX_BONUS / this.getDuration().toSeconds();
        final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
        final long gains = (long) Math.ceil(Constants.MIN_GAINS + remainingDuration.toSeconds() * coinsPerSec);

        this.stop();

        TRIVIA_SUMMARY.labels("win").observe(gains);

        return new Player(this.getContext().getGuildId(), member.getId())
                .win(gains)
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CLAP + " (**%s**) Correct ! You won " +
                        "**%s**.", member.getUsername(), FormatUtils.coins(gains)), channel));
    }

    public void hasAnswered(Snowflake userId) {
        final TriviaPlayer player = new TriviaPlayer(this.getContext().getGuildId(), userId);
        if (!this.addPlayerIfAbsent(player)) {
            this.getPlayers().get(userId).setAnswered(true);
        }
    }

    public List<String> getAnswers() {
        return Collections.unmodifiableList(this.answers);
    }

    public String getCorrectAnswer() {
        return this.trivia.getCorrectAnswer();
    }

}
