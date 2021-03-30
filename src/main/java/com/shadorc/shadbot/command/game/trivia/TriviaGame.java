package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.api.json.trivia.TriviaResponse;
import com.shadorc.shadbot.api.json.trivia.TriviaResult;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.game.player.Player;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TriviaGame extends MultiplayerGame<TriviaPlayer> {

    @Nullable
    private final Integer categoryId;
    private final List<String> answers;

    private TriviaResult trivia;
    private Instant startTimer;

    // Trivia API doc : https://opentdb.com/api_config.php
    public TriviaGame(TriviaCmd gameCmd, Context context, @Nullable Integer categoryId) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.categoryId = categoryId;
        this.answers = new ArrayList<>();
    }

    @Override
    public Mono<Void> start() {
        return Mono.defer(() -> {
            final String url = "https://opentdb.com/api.php"
                    + "?amount=1"
                    + "&category=%s".formatted(Objects.toString(this.categoryId, ""));
            return RequestHelper.fromUrl(url)
                    .to(TriviaResponse.class)
                    .map(TriviaResponse::getResults)
                    .map(list -> list.get(0))
                    .doOnNext(trivia -> {
                        this.trivia = trivia;

                        if ("multiple".equals(trivia.getType())) {
                            this.answers.addAll(trivia.getIncorrectAnswers());
                            this.answers.add(trivia.getCorrectAnswer());
                            Collections.shuffle(this.answers);
                        } else {
                            this.answers.addAll(List.of("True", "False"));
                        }

                        this.schedule(this.end());
                        this.startTimer = Instant.now();
                        TriviaInputs.create(this.getContext().getClient(), this).listen();
                    })
                    .then();
        });
    }

    @Override
    public Mono<Void> end() {
        return this.context.reply(Emoji.HOURGLASS, this.context.localize("trivia.time.elapsed")
                .formatted(this.trivia.getCorrectAnswer()))
                .then(Mono.fromRunnable(this::destroy));
    }

    @Override
    public Mono<MessageData> show() {
        return Mono.defer(() -> {
            final String description = "**%s**\n%s"
                    .formatted(this.trivia.getQuestion(),
                            FormatUtil.numberedList(this.answers.size(), this.answers.size(),
                                    count -> "\t**%d**. %s".formatted(count, this.answers.get(count - 1))));

            final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtil.getDefaultEmbed(
                    embed -> embed.setAuthor(this.context.localize("trivia.title"), null, this.context.getAuthorAvatar())
                            .setDescription(description)
                            .addField(this.context.localize("trivia.category"),
                                    "`%s`".formatted(this.trivia.getCategory()), true)
                            .addField(this.context.localize("trivia.difficulty"),
                                    "`%s`".formatted(this.trivia.getDifficulty()), true)
                            .setFooter(this.context.localize("trivia.footer")
                                    .formatted(this.duration.toSeconds(), Emoji.RED_CROSS), null));

            return this.context.reply(embedConsumer);
        });
    }

    protected Mono<MessageData> win(Member member) {
        final double coinsPerSec = (double) Constants.MAX_BONUS / this.duration.toSeconds();
        final long remainingSec = this.duration.minus(TimeUtil.elapsed(this.startTimer)).toSeconds();
        final long gains = (long) Math.ceil(Constants.MIN_GAINS + remainingSec * coinsPerSec);

        this.destroy();

        Telemetry.TRIVIA_SUMMARY.labels("win").observe(gains);

        return new Player(this.context.getGuildId(), member.getId())
                .win(gains)
                .then(this.context.reply(Emoji.CLAP, this.context.localize("trivia.win")
                        .formatted(this.context.localize(gains))));
    }

    public void hasAnswered(Snowflake userId) {
        final TriviaPlayer player = new TriviaPlayer(this.context.getGuildId(), userId);
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
