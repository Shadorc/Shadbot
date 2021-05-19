package com.locibot.locibot.command.game.trivia;

import com.locibot.locibot.api.json.trivia.TriviaResponse;
import com.locibot.locibot.api.json.trivia.TriviaResult;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.MultiplayerGame;
import com.locibot.locibot.core.game.player.Player;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
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
    public TriviaGame(Context context, @Nullable Integer categoryId) {
        super(context, Duration.ofSeconds(30));
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
                    .map(TriviaResponse::results)
                    .map(list -> list.get(0))
                    .doOnNext(trivia -> {
                        this.trivia = trivia;

                        if ("multiple".equals(trivia.type())) {
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
        return Mono.fromRunnable(() -> Telemetry.TRIVIA_SUMMARY.labels("loss").observe(0))
                .then(this.context.createFollowupMessage(Emoji.HOURGLASS, this.context.localize("trivia.time.elapsed")
                        .formatted(this.trivia.getCorrectAnswer())))
                .then(Mono.fromRunnable(this::destroy));
    }

    @Override
    public Mono<Message> show() {
        return Mono.defer(() -> {
            final String description = "**%s**\n**%s**\n%s"
                    .formatted(this.context.localize("trivia.description"), this.trivia.getQuestion(),
                            FormatUtil.numberedList(this.answers.size(), this.answers.size(),
                                    count -> "\t**%d**. %s".formatted(count, this.answers.get(count - 1))));

            final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtil.getDefaultEmbed(
                    embed -> embed.setAuthor(this.context.localize("trivia.title"), null, this.context.getAuthorAvatar())
                            .setDescription(description)
                            .addField(this.context.localize("trivia.category"),
                                    "`%s`".formatted(this.trivia.category()), true)
                            .addField(this.context.localize("trivia.difficulty"),
                                    "`%s`".formatted(this.trivia.difficulty()), true)
                            .setFooter(this.context.localize("trivia.footer")
                                    .formatted(this.duration.toSeconds(), Emoji.RED_CROSS), null));

            return this.context.createFollowupMessage(embedConsumer);
        });
    }

    protected Mono<Message> win(Member member) {
        final double coinsPerSec = (double) Constants.MAX_BONUS / this.duration.toSeconds();
        final long remainingSec = this.duration.minus(TimeUtil.elapsed(this.startTimer)).toSeconds();
        final long gains = (long) Math.ceil(Constants.MIN_GAINS + remainingSec * coinsPerSec);

        this.destroy();

        Telemetry.TRIVIA_SUMMARY.labels("win").observe(gains);

        return new Player(this.context.getGuildId(), member.getId())
                .win(gains)
                .then(this.context.createFollowupMessage(Emoji.CLAP, this.context.localize("trivia.win")
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
