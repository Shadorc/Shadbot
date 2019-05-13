package me.shadorc.shadbot.command.game.trivia;

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
import me.shadorc.shadbot.core.game.MultiplayerGame;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TriviaGame extends MultiplayerGame<TriviaPlayer> {

    protected static final int MIN_GAINS = 100;
    protected static final int MAX_BONUS = 150;

    private final TriviaResult trivia;
    private final List<String> answers;

    private long startTime;

    // Trivia API doc : https://opentdb.com/api_config.php
    public TriviaGame(GameCmd<TriviaGame> gameCmd, Context context, Integer categoryId) {
        super(gameCmd, context, Duration.ofSeconds(30));

        final String url = String.format("https://opentdb.com/api.php?amount=1&category=%s", Objects.toString(categoryId, ""));
        final TriviaResponse response = NetUtils.readValue(url, TriviaResponse.class);
        this.trivia = response.getResults().get(0);

        this.answers = new ArrayList<>();
        if ("multiple".equals(this.trivia.getType())) {
            this.answers.addAll(this.trivia.getIncorrectAnswers());
            this.answers.add(this.trivia.getCorrectAnswer());
            Collections.shuffle(this.answers);
        } else {
            this.answers.addAll(List.of("True", "False"));
        }
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
                        .setFooter(String.format("You have %d seconds to answer. Use %scancel to force the stop.",
                                this.getDuration().toSeconds(), this.getContext().getPrefix()), null));

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
        if (this.getPlayers().containsKey(userId)) {
            this.getPlayers().get(userId).setAnswered(true);
        } else {
            this.addPlayerIfAbsent(new TriviaPlayer(userId));
        }
    }

    public List<String> getAnswers() {
        return Collections.unmodifiableList(this.answers);
    }

    public String getCorrectAnswer() {
        return this.trivia.getCorrectAnswer();
    }

}
