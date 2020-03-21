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
import discord4j.core.object.util.Snowflake;
import discord4j.discordjson.json.*;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
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

    public HangmanGame(HangmanCmd gameCmd, Context context, HangmanCmd.Difficulty difficulty) {
        super(gameCmd, context, Duration.ofMinutes(3));
        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(1));
        this.messageId = new AtomicLong(-1);
        this.word = gameCmd.getWord(difficulty);
        this.lettersTested = new ArrayList<>();
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
        return Mono.just(new StringBuilder())
                .flatMap(strBuilder -> {
                    if (this.failCount == IMG_LIST.size()) {
                        return Mono.just(strBuilder.append(
                                String.format(Emoji.THUMBSDOWN + " (**%s**) You lose, the word to guess was **%s** !",
                                        this.getContext().getUsername(), this.word)));
                    } else {
                        final float bonusPerImg = (float) MAX_BONUS / IMG_LIST.size();
                        final float imagesRemaining = IMG_LIST.size() - this.failCount;
                        final int gains = (int) Math.ceil(MIN_GAINS + bonusPerImg * imagesRemaining);

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

        final ImmutableEmbedData.Builder embedData = DiscordUtils.getDefaultEmbedData();

        final ImmutableEmbedAuthorData authorData = ImmutableEmbedAuthorData.builder()
                .name(Possible.of("Hangman Game"))
                .iconUrl(Possible.of(this.getContext().getAvatarUrl()))
                .build();

        final ImmutableEmbedThumbnailData thumbnaildata = ImmutableEmbedThumbnailData.builder()
                .url(Possible.of("https://i.imgur.com/Vh9WyaU.png"))
                .build();

        final List<EmbedFieldData> fieldDataList = new ArrayList<>();
        fieldDataList.add(
                ImmutableEmbedFieldData.of("Word", this.getRepresentation(this.word), Possible.of(false)));

        embedData.author(Possible.of(authorData))
                .thumbnail(Possible.of(thumbnaildata))
                .description(Possible.of("Type letters or enter a word if you think you've guessed it."));

        if (!missedLetters.isEmpty()) {
            fieldDataList.add(
                    ImmutableEmbedFieldData.of("Misses", String.join(", ", missedLetters), Possible.of(false)));
        }

        embedData.fields(Possible.of(fieldDataList));

        if (this.isScheduled()) {
            final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
            final ImmutableEmbedFooterData footerData = ImmutableEmbedFooterData.builder()
                    .text(String.format("Will automatically stop in %s seconds. Use %scancel to force the stop.",
                            remainingDuration.toSeconds(), this.getContext().getPrefix()))
                    .build();
            embedData.footer(Possible.of(footerData));
        } else {
            final ImmutableEmbedFooterData footerData = ImmutableEmbedFooterData.builder()
                    .text("Finished.")
                    .build();
            embedData.footer(Possible.of(footerData));
        }

        if (this.failCount > 0) {
            final ImmutableEmbedImageData imageData = ImmutableEmbedImageData.builder()
                    .url(Possible.of(IMG_LIST.get(Math.min(IMG_LIST.size(), this.failCount) - 1)))
                    .build();
            embedData.image(Possible.of(imageData));
        }

        return this.getContext()
                .getClient()
                .rest()
                .getMessageById(this.getContext().getChannelId(), Snowflake.of(this.messageId.get()))
                .edit(ImmutableMessageEditRequest.builder().embed(Possible.of(Optional.of(embedData.build()))).build())
                .onErrorResume(err -> this.getContext()
                        .getChannel()
                        .flatMap(channel -> channel.getRestChannel()
                                .createMessage(ImmutableMessageCreateRequest.builder()
                                        .embed(Possible.of(embedData.build()))
                                        .build())))
                .doOnNext(message -> this.messageId.set(Long.parseLong(message.id())))
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