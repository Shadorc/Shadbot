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
import discord4j.core.object.entity.Message;
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
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .zipWith(DatabaseManager.getGuilds().getDBGuild(this.guildId).map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((guildMusic, locale) -> {
                    final String message = Emoji.MUSICAL_NOTE + " " + I18nManager.localize(locale, "trackevent.playing")
                            .formatted(FormatUtil.trackName(track.getInfo()));
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtil.sendMessage(message, channel));
                }))
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .filter(__ -> endReason == AudioTrackEndReason.FINISHED)
                // Everything seems fine, reset error counter.
                .doOnNext(__ -> this.errorCount.set(0))
                .flatMap(__ -> this.nextOrEnd())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Telemetry.MUSIC_ERROR_COUNTER.labels(exception.getClass().getSimpleName()).inc();
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
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
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.info("{Guild ID: {}} Music stuck, skipping it", this.guildId.asLong());
        Telemetry.MUSIC_ERROR_COUNTER.labels("StuckException").inc();
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll "
                        + "try to play the next available song.", channel))
                .then(this.nextOrEnd())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * Start the next track or end the guild music if this is the end of the playlist.
     *
     * @return A {@link Mono} that completes when a new track has been started or when the guild music ended.
     */
    private Mono<Message> nextOrEnd() {
        return Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                // If the next track could not be started
                .filter(guildMusic -> !guildMusic.getTrackScheduler().nextTrack())
                .flatMap(GuildMusic::end);
    }

}
