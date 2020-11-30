package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class TrackEventListener extends AudioEventAdapter {

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
                            FormatUtils.trackName(track.getInfo()));
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(message, channel));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .filter(ignored -> endReason == AudioTrackEndReason.FINISHED)
                // Everything seems fine, reset error counter.
                .doOnNext(ignored -> this.errorCount.set(0))
                .flatMap(ignored -> this.nextOrEnd())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .flatMap(guildMusic -> {
                    this.errorCount.incrementAndGet();

                    final String errMessage = ShadbotUtils.cleanLavaplayerErr(exception);
                    LOGGER.info("{Guild ID: {}} {}Track exception: {}", this.guildId.asLong(),
                            this.errorCount.get() > 3 ? "(Ignored) " : "", errMessage);

                    final StringBuilder strBuilder = new StringBuilder();
                    if (this.errorCount.get() <= 3) {
                        strBuilder.append(String.format(Emoji.RED_CROSS + " Sorry, %s. I'll try to play "
                                + "the next available song.", errMessage.toLowerCase()));
                    }

                    if (this.errorCount.get() == 3) {
                        LOGGER.info("{Guild ID: {}} Too many errors in a row. They will be ignored until"
                                + " a music can be played.", this.guildId.asLong());
                        strBuilder.append("\n" + Emoji.RED_FLAG + " Too many errors in a row, I will ignore"
                                + " them until I find a music that can be played.");
                    }

                    return guildMusic.getMessageChannel()
                            .filter(ignored -> !strBuilder.isEmpty())
                            .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
                            .then(this.nextOrEnd());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.info("{Guild ID: {}} Music stuck, skipping it", this.guildId.asLong());
        Mono.justOrEmpty(MusicManager.getInstance().getGuildMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll "
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
