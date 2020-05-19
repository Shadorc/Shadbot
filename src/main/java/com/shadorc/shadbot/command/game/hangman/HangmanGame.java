package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.Game;
import com.shadorc.shadbot.core.game.player.Player;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Snowflake;
import io.prometheus.client.Summary;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HangmanGame extends Game<HangmanCmd> {

    protected static final int MIN_GAINS = 500;
    protected static final int MAX_BONUS = 1000;

    private static final Summary HANGMAN_SUMMARY = Summary.build().name("game_hangman").help("Hangman game")
            .labelNames("result").register();
    private static final List<String> IMG_LIST = List.of(
            HangmanGame.getImageUrl("8/8b", 0),
            HangmanGame.getImageUrl("3/30", 1),
            HangmanGame.getImageUrl("7/70", 2),
            HangmanGame.getImageUrl("9/97", 3),
            HangmanGame.getImageUrl("2/27", 4),
            HangmanGame.getImageUrl("6/6b", 5),
            HangmanGame.getImageUrl("d/d6", 6));
    private static final float BONUS_PER_IMAGE = (float) MAX_BONUS / IMG_LIST.size();

    private final HangmanCmd.Difficulty difficulty;
    private final RateLimiter rateLimiter;
    private final AtomicLong messageId;
    private final String word;
    private final Set<String> lettersTested;

    private long startTime;
    private int failCount;

    public HangmanGame(HangmanCmd gameCmd, Context context, HangmanCmd.Difficulty difficulty) {
        super(gameCmd, context, Duration.ofMinutes(3));
        this.difficulty = difficulty;
        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(1));
        this.messageId = new AtomicLong(-1);
        this.word = this.getWord(difficulty);
        this.lettersTested = new HashSet<>();
        this.failCount = 0;
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(Mono.fromRunnable(this::stop));
            this.startTime = System.currentTimeMillis();
            new HangmanInputs(this.getContext().getClient(), this).subscribe();
        });
    }

    @Override
    public Mono<Void> end() {
        return Mono.defer(() -> {
            final StringBuilder strBuilder = new StringBuilder();
            if (this.failCount == IMG_LIST.size()) {
                return Mono.just(strBuilder.append(
                        String.format(Emoji.THUMBSDOWN + " (**%s**) You lose, the word to guess was **%s** !",
                                this.getContext().getUsername(), this.word)));
            } else {
                final float imagesRemaining = IMG_LIST.size() - this.failCount;
                final int difficultyMultiplicator = this.difficulty == HangmanCmd.Difficulty.HARD ? 4 : 1;
                final int gains = (int) (MIN_GAINS + Math.ceil(BONUS_PER_IMAGE * imagesRemaining) * difficultyMultiplicator);
                HANGMAN_SUMMARY.labels("win").observe(gains);
                return new Player(this.getContext().getGuildId(), this.getContext().getAuthorId())
                        .win(gains)
                        .thenReturn(strBuilder.append(
                                String.format(Emoji.PURSE + " (**%s**) Well played, you found the word ! You won **%s**.",
                                        this.getContext().getUsername(), FormatUtils.coins(gains))));
            }
        })
                .map(StringBuilder::toString)
                .flatMap(text -> this.show()
                        .then(this.getContext().getChannel())
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                        .then(Mono.fromRunnable(this::stop)));
    }

    @Override
    public Mono<Void> show() {
        final List<String> missedLetters = this.lettersTested.stream()
                .filter(letter -> !this.word.contains(letter))
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor("Hangman Game", null, this.getContext().getAvatarUrl());
                    embed.setThumbnail("https://i.imgur.com/Vh9WyaU.png");
                    embed.addField("Word", this.getRepresentation(this.word), false);
                    embed.setDescription("Type letters or enter a word if you think you've guessed it.");

                    if (!missedLetters.isEmpty()) {
                        embed.addField("Misses", String.join(", ", missedLetters), false);
                    }

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration()
                                .minusMillis(TimeUtils.getMillisUntil(this.startTime));
                        embed.setFooter(String.format("Will automatically stop in %s seconds. Use %scancel to force the stop.",
                                remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                    } else {
                        embed.setFooter("Finished.", null);
                    }

                    if (this.failCount > 0) {
                        embed.setImage(IMG_LIST.get(Math.min(IMG_LIST.size(), this.failCount) - 1));
                    }
                });

        return this.getContext().getClient()
                .getMessageById(this.getContext().getChannelId(), Snowflake.of(this.messageId.get()))
                .flatMap(message -> message.edit(spec -> spec.setEmbed(embedConsumer)))
                .switchIfEmpty(Mono.error(new RuntimeException("Message not found.")))
                // An error can occur if the message is not found or if the message id is -1
                .onErrorResume(err -> this.getContext().getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
                .map(Message::getId)
                .map(Snowflake::asLong)
                .doOnNext(this.messageId::set)
                .then();
    }

    private String getWord(HangmanCmd.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return this.getGameCmd().getEasyWords().getRandomWord();
            case HARD:
                return this.getGameCmd().getHardWords().getRandomWord();
            default:
                throw new RuntimeException(String.format("Unknown difficulty: %s", difficulty));
        }
    }

    protected Mono<Void> checkLetter(String chr) {
        if (this.lettersTested.contains(chr)) {
            return Mono.empty();
        }

        if (!this.word.contains(chr)) {
            this.failCount++;
            if (this.failCount == IMG_LIST.size()) {
                return this.end();
            }
        }

        this.lettersTested.add(chr);

        // The word has been entirely guessed
        if (StringUtils.remove(this.getRepresentation(this.word), "\\", " ", "*").equalsIgnoreCase(this.word)) {
            return this.end();
        }

        return this.show();
    }

    protected Mono<Void> checkWord(String word) {
        // If the word has been guessed
        if (this.word.equalsIgnoreCase(word)) {
            this.lettersTested.addAll(StringUtils.split(word, ""));
            return this.end();
        }

        this.failCount++;
        if (this.failCount == IMG_LIST.size()) {
            return this.end();
        }
        return this.show();
    }

    public RateLimiter getRateLimiter() {
        return this.rateLimiter;
    }

    public String getWord() {
        return this.word;
    }

    private String getRepresentation(String word) {
        return String.format("**%s**",
                FormatUtils.format(StringUtils.split(word, ""),
                        letter -> this.lettersTested.contains(letter) ? letter.toUpperCase() : "\\_", " "));
    }

    private static String getImageUrl(String path, int num) {
        return String.format("https://upload.wikimedia.org/wikipedia/commons/thumb/%s/Hangman-%d.png/60px-Hangman-%d.png",
                path, num, num);
    }

}