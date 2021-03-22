package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class TrackEventListener extends AudioEventAdapter {

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
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
        Mono.zip(
                Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                        .flatMap(GuildMusic::getMessageChannel),
                DatabaseManager.getGuilds().getDBGuild(this.guildId)
                        .map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((channel, locale) -> DiscordUtil.sendMessage(Emoji.MUSICAL_NOTE,
                        I18nManager.localize(locale, "trackevent.playing")
                                .formatted(FormatUtil.trackName(track.getInfo())), channel)))
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

        Mono.zip(
                Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId)),
                DatabaseManager.getGuilds().getDBGuild(this.guildId)
                        .map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((guildMusic, locale) -> {
                    if (errorCount > MAX_ERROR_COUNT) {
                        LOGGER.error("{Guild ID: {}} Stopping playlist due to too many errors.", this.guildId.asString());
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_FLAG,
                                        I18nManager.localize(locale, "trackevent.stop.retrying"), channel))
                                .then(guildMusic.end());
                    }

                    final String errMessage = ShadbotUtil.cleanLavaplayerErr(exception);
                    LOGGER.info("{Guild ID: {}} Track exception ({}/{}): {}",
                            this.guildId.asString(), errorCount, IGNORED_ERROR_THRESHOLD, errMessage);

                    if (errorCount < IGNORED_ERROR_THRESHOLD) {
                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_CROSS,
                                        I18nManager.localize(locale, "trackevent.exception"), channel))
                                .then(this.nextOrEnd(guildMusic));
                    } else if (errorCount == IGNORED_ERROR_THRESHOLD) {
                        LOGGER.info("{Guild ID: {}} Too many errors in a row. They will be ignored until a music can be played.",
                                this.guildId.asString());

                        return guildMusic.getMessageChannel()
                                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_FLAG,
                                        I18nManager.localize(locale, "trackevent.too.many.exceptions"), channel))
                                .then(this.nextOrEnd(guildMusic));
                    }

                    return Mono.empty();
                }))
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.info("{Guild ID: {}} Music stuck, skipping it", this.guildId.asLong());
        Telemetry.MUSIC_ERROR_COUNTER.labels("StuckException").inc();

        Mono.zip(
                Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId)),
                DatabaseManager.getGuilds().getDBGuild(this.guildId)
                        .map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((guildMusic, locale) -> guildMusic.getMessageChannel()
                        .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_EXCLAMATION,
                                I18nManager.localize(locale, "trackevent.stuck"), channel))
                        .then(this.nextOrEnd(guildMusic))))
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
