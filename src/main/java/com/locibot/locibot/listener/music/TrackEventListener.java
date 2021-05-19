package com.locibot.locibot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.music.GuildMusic;
import com.locibot.locibot.music.MusicManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static com.locibot.locibot.music.MusicManager.LOGGER;

public class TrackEventListener extends AudioEventAdapter {

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final int IGNORED_ERROR_THRESHOLD = 3;
    private static final int MAX_ERROR_COUNT = 10;

    private final Locale locale;
    private final Snowflake guildId;
    private final AtomicInteger errorCount;

    public TrackEventListener(Locale locale, Snowflake guildId) {
        this.locale = locale;
        this.guildId = guildId;
        this.errorCount = new AtomicInteger();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.MUSICAL_NOTE,
                        I18nManager.localize(this.locale, "trackevent.playing")
                                .formatted(FormatUtil.trackName(this.locale, track.getInfo())), channel))
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Ignore events coming from exceptions
        if (endReason != AudioTrackEndReason.FINISHED) {
            return;
        }

        // Everything seems fine, reset error counter.
        this.errorCount.set(0);

        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(this::nextOrEnd)
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Telemetry.MUSIC_ERROR_COUNTER.labels(exception.getClass().getSimpleName()).inc();
        final int errorCount = this.errorCount.incrementAndGet();

        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(guildMusic -> {
                    if (errorCount > MAX_ERROR_COUNT) {
                        LOGGER.error("{Guild ID: {}} Stopping playlist due to too many errors.", this.guildId.asString());
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_FLAG,
                                        I18nManager.localize(this.locale, "trackevent.stop.retrying"), channel))
                                .then(guildMusic.end());
                    }

                    final String errMessage = ShadbotUtil.cleanLavaplayerErr(exception);
                    LOGGER.info("{Guild ID: {}} Track exception ({}/{}): {}",
                            this.guildId.asString(), errorCount, IGNORED_ERROR_THRESHOLD, errMessage);

                    if (errorCount < IGNORED_ERROR_THRESHOLD) {
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_CROSS,
                                        I18nManager.localize(this.locale, "trackevent.exception"), channel))
                                .then(this.nextOrEnd(guildMusic));
                    } else if (errorCount == IGNORED_ERROR_THRESHOLD) {
                        LOGGER.info("{Guild ID: {}} Too many errors in a row. They will be ignored until a music can be played.",
                                this.guildId.asString());

                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_FLAG,
                                        I18nManager.localize(this.locale, "trackevent.too.many.exceptions"), channel))
                                .then(this.nextOrEnd(guildMusic));
                    }

                    return Mono.empty();
                })
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.info("{Guild ID: {}} Music stuck, skipping it", this.guildId.asLong());
        Telemetry.MUSIC_ERROR_COUNTER.labels("StuckException").inc();

        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(guildMusic -> guildMusic.getMessageChannel()
                        .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_EXCLAMATION,
                                I18nManager.localize(this.locale, "trackevent.stuck"), channel))
                        .then(this.nextOrEnd(guildMusic)))
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * Start the next track or end the guild music if this is the end of the playlist.
     *
     * @return A {@link Mono} that completes when a new track has been started or when the guild music ended.
     */
    private Mono<Void> nextOrEnd(GuildMusic guildMusic) {
        if (guildMusic.getTrackScheduler().nextTrack()) {
            return Mono.empty();
        }
        // If the next track could not be started
        return guildMusic.end().then();
    }

}
