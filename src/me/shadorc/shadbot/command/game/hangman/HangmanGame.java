package me.shadorc.shadbot.command.game.hangman;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.command.game.hangman.HangmanCmd.Difficulty;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.Game;
import me.shadorc.shadbot.core.game.player.Player;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HangmanGame extends Game {

    private static final List<String> IMG_LIST = List.of(
            HangmanGame.getImageUrl("8/8b", 0),
            HangmanGame.getImageUrl("3/30", 1),
            HangmanGame.getImageUrl("7/70", 2),
            HangmanGame.getImageUrl("9/97", 3),
            HangmanGame.getImageUrl("2/27", 4),
            HangmanGame.getImageUrl("6/6b", 5),
            HangmanGame.getImageUrl("d/d6", 6));

    protected static final int MIN_GAINS = 200;
    protected static final int MAX_BONUS = 200;

    private final RateLimiter rateLimiter;
    private final AtomicLong messageId;
    private final String word;
    private final List<String> lettersTested;

    private long startTime;
    private int failCount;

    public HangmanGame(HangmanCmd gameCmd, Context context, Difficulty difficulty) {
        super(gameCmd, context, Duration.ofMinutes(3));
        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(1));
        this.messageId = new AtomicLong(-1);
        this.word = gameCmd.getWord(difficulty);
        this.lettersTested = new ArrayList<>();
        this.failCount = 0;
    }

    @Override
    public void start() {
        this.schedule(Mono.fromRunnable(this::stop));
        this.startTime = System.currentTimeMillis();
        new HangmanInputs(this.getContext().getClient(), this).subscribe();
    }

    @Override
    public Mono<Void> end() {
        final StringBuilder strBuilder = new StringBuilder();
        if (this.failCount == IMG_LIST.size()) {
            strBuilder.append(String.format(Emoji.THUMBSDOWN + " (**%s**) You lose, the word to guess was **%s** !",
                    this.getContext().getUsername(), this.word));
        } else {
            final float bonusPerImg = (float) MAX_BONUS / IMG_LIST.size();
            final float imagesRemaining = IMG_LIST.size() - this.failCount;
            final int gains = (int) Math.ceil(MIN_GAINS + bonusPerImg * imagesRemaining);

            new Player(this.getContext().getGuildId(), this.getContext().getAuthorId()).win(gains);

            strBuilder.append(String.format(Emoji.PURSE + " (**%s**) Well played, you found the word ! You won **%s**.",
                    this.getContext().getUsername(), FormatUtils.coins(gains)));
        }

        return this.show()
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
                .then(Mono.fromRunnable(this::stop));
    }

    @Override
    public Mono<Void> show() {
        final List<String> missedLetters = this.lettersTested.stream()
                .filter(letter -> !this.word.contains(letter))
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor("Hangman Game", null, this.getContext().getAvatarUrl())
                            .setThumbnail("https://lh5.ggpht.com/nIoJylIWCj1gKv9dxtd4CFE2aeXvG7MbvP0BNFTtTFusYlxozJRQmHizsIDxydaa7DHT=w300")
                            .setDescription("Type letters or enter a word if you think you've guessed it.")
                            .addField("Word", this.getRepresentation(this.word), false);

                    if (!missedLetters.isEmpty()) {
                        embed.addField("Misses", String.join(", ", missedLetters), false);
                    }

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
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
                .onErrorResume(err -> this.getContext().getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
                .doOnNext(message -> this.messageId.set(message.getId().asLong()))
                .then();
    }

    public Mono<Void> checkLetter(String chr) {
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

    public Mono<Void> checkWord(String word) {
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