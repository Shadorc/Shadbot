package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.game.player.Player;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HangmanGame extends MultiplayerGame<Player> {

    private static final List<String> IMG_LIST = List.of(
            HangmanGame.buildImageUrl("8/8b", 0),
            HangmanGame.buildImageUrl("3/30", 1),
            HangmanGame.buildImageUrl("7/70", 2),
            HangmanGame.buildImageUrl("9/97", 3),
            HangmanGame.buildImageUrl("2/27", 4),
            HangmanGame.buildImageUrl("6/6b", 5),
            HangmanGame.buildImageUrl("d/d6", 6));
    private static final float BONUS_PER_IMAGE = (float) Constants.MAX_BONUS / IMG_LIST.size();

    private final HangmanCmd.Difficulty difficulty;
    private final RateLimiter rateLimiter;
    private final String word;
    private final Set<String> lettersTested;

    private Instant startTimer;
    private int failCount;

    public HangmanGame(Context context, HangmanCmd.Difficulty difficulty, String word) {
        super(context, Duration.ofMinutes(3));
        this.difficulty = difficulty;
        this.word = word;
        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(1));
        this.lettersTested = new HashSet<>();
        this.failCount = 0;
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.show()
                    .then(Mono.fromRunnable(this::destroy)));
            this.startTimer = Instant.now();
            HangmanInputs.create(this.getContext().getClient(), this).listen();
        });
    }

    @Override
    public Mono<Message> show() {
        return Mono.
                fromCallable(() -> {
                    final EmbedCreateSpec.Builder embed = ShadbotUtil.createEmbedBuilder()
                            .author(this.context.localize("hangman.title"), null, this.getContext().getAuthorAvatar())
                            .thumbnail("https://i.imgur.com/Vh9WyaU.png")
                            .description(this.context.localize("hangman.description")
                                    .formatted(this.context.getCommandName(), this.context.getSubCommandGroupName().orElseThrow(),
                                            HangmanCmd.JOIN_SUB_COMMAND))
                            .addField(this.context.localize("hangman.word"), this.getRepresentation(this.word), false);

                    final List<String> missedLetters = this.lettersTested.stream()
                            .filter(letter -> !this.word.contains(letter))
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());
                    if (!missedLetters.isEmpty()) {
                        embed.addField(this.context.localize("hangman.misses"),
                                String.join(", ", missedLetters), false);
                    }

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration().minus(TimeUtil.elapsed(this.startTimer));
                        embed.footer(this.context.localize("hangman.footer.remaining")
                                .formatted(remainingDuration.toSeconds()), null);
                    } else {
                        embed.footer(this.context.localize("hangman.footer.finished")
                                .formatted(this.word), null);
                    }

                    if (this.failCount > 0) {
                        embed.image(IMG_LIST.get(Math.min(IMG_LIST.size(), this.failCount) - 1));
                    }

                    return embed.build();
                })
                .flatMap(this.context::editFollowupMessage);
    }

    @Override
    public Mono<Void> end() {
        return Mono.
                defer(() -> {
                    if (this.failCount == IMG_LIST.size()) {
                        return this.context.createFollowupMessage(Emoji.THUMBSDOWN, this.context.localize("hangman.lose")
                                .formatted(this.word));
                    } else {
                        final float imagesRemaining = IMG_LIST.size() - this.failCount;
                        final int difficultyMultiplicator = this.difficulty == HangmanCmd.Difficulty.HARD ? 4 : 1;
                        final int gains = (int) (Constants.MIN_GAINS + Math.ceil(BONUS_PER_IMAGE * imagesRemaining) * difficultyMultiplicator);

                        Telemetry.HANGMAN_SUMMARY.labels("win").observe(gains);
                        return Flux.fromIterable(this.players.values())
                                .flatMap(player -> player.win(gains))
                                .then(this.context.createFollowupMessage(Emoji.PURSE, this.context.localize("hangman.win")
                                        .formatted(this.context.localize(gains))));
                    }
                })
                .then(Mono.fromRunnable(this::destroy));
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
        if (this.getRepresentation(this.word).replace("\\", "")
                .replace(" ", "")
                .replace("*", "").equalsIgnoreCase(this.word)) {
            return this.end();
        }

        return this.show().then();
    }

    protected Mono<Void> checkWord(String word) {
        // If the word has been guessed
        if (this.word.equalsIgnoreCase(word)) {
            this.lettersTested.addAll(StringUtil.split(word, ""));
            return this.end();
        }

        this.failCount++;
        if (this.failCount == IMG_LIST.size()) {
            return this.end();
        }
        return this.show().then();
    }

    private static String buildImageUrl(String path, int num) {
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/%s/Hangman-%d.png/60px-Hangman-%d.png"
                .formatted(path, num, num);
    }

    public RateLimiter getRateLimiter() {
        return this.rateLimiter;
    }

    public String getWord() {
        return this.word;
    }

    private String getRepresentation(String word) {
        return "**%s**".formatted(FormatUtil.format(StringUtil.split(word, ""),
                letter -> this.lettersTested.contains(letter) ? letter.toUpperCase() : "\\_", " "));
    }

}
