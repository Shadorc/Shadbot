package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class TrackEventListener extends AudioEventAdapter {

    private static final int IGNORED_ERROR_THRESHOLD = 3;
    private static final int MAX_ERROR_COUNT = 10;

    private final Snowflake guildId;
    private final AtomicInteger errorCount;

    public TrackEventListener(Snowflake guildId) {
        this.guildId = guildId;
        this.errorCount = new AtomicInteger();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .flatMap(guildMusic -> {
                    final String message = String.format(Emoji.MUSICAL_NOTE + " Currently playing: **%s**",
                            FormatUtil.trackName(track.getInfo()));
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtil.sendMessage(message, channel));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .filter(__ -> endReason == AudioTrackEndReason.FINISHED)
                // Everything seems fine, reset error counter.
                .doOnNext(__ -> this.errorCount.set(0))
                .flatMap(__ -> this.nextOrEnd())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Telemetry.MUSIC_ERROR_COUNTER.labels(exception.getClass().getSimpleName()).inc();
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .flatMap(guildMusic -> {
                    this.errorCount.incrementAndGet();
                    if (this.errorCount.get() > MAX_ERROR_COUNT) {
                        LOGGER.error("{Guild ID: {}} Stopping playlist due to too many errors.", this.guildId.asLong());
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_FLAG
                                        + " Something is going wrong, I will stop retrying. Please try again later.", channel))
                                .then(guildMusic.end());
                    }

                    final String errMessage = ShadbotUtil.cleanLavaplayerErr(exception);
                    LOGGER.info("{Guild ID: {}} {}Track exception: {}", this.guildId.asLong(),
                            this.errorCount.get() > IGNORED_ERROR_THRESHOLD ? "(Ignored) " : "", errMessage);

                    final StringBuilder strBuilder = new StringBuilder();
                    if (this.errorCount.get() < IGNORED_ERROR_THRESHOLD) {
                        strBuilder.append(String.format(Emoji.RED_CROSS + " Sorry, %s. I'll try to play "
                                + "the next available song.", errMessage.toLowerCase()));
                    }

                    if (this.errorCount.get() == IGNORED_ERROR_THRESHOLD) {
                        LOGGER.info("{Guild ID: {}} Too many errors in a row. They will be ignored until"
                                + " a music can be played.", this.guildId.asLong());
                        strBuilder.append("\n" + Emoji.RED_FLAG + " Too many errors in a row, I will ignore"
                                + " them until I find a music that can be played.");
                    }

                    return guildMusic.getMessageChannel()
                            .filter(__ -> !strBuilder.isEmpty())
                            .flatMap(channel -> DiscordUtil.sendMessage(strBuilder.toString(), channel))
                            .then(this.nextOrEnd());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.info("{Guild ID: {}} Music stuck, skipping it", this.guildId.asLong());
        Telemetry.MUSIC_ERROR_COUNTER.labels("StuckException").inc();
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll "
                        + "try to play the next available song.", channel))
                .then(this.nextOrEnd())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * Start the next track or end the guild music if this is the end of the playlist.
     *
     * @return A {@link Mono} that completes when a new track has been started or when the guild music ended.
     */

    private Mono<Void> nextOrEnd() {
        return Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                // If the next track could not be started
                .filter(guildMusic -> !guildMusic.getTrackScheduler().nextTrack())
                .flatMap(GuildMusic::end);
    }

}
