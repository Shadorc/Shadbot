package com.shadorc.shadbot.music;

import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class GuildMusic {

    private final DiscordClient client;
    private final Snowflake guildId;
    private final TrackScheduler trackScheduler;

    private final Map<AudioLoadResultListener, Future<Void>> listeners;
    private final AtomicBoolean isWaitingForChoice;
    private final AtomicBoolean isLeavingScheduled;
    private final AtomicLong messageChannelId;
    private final AtomicLong djId;

    public GuildMusic(DiscordClient client, Snowflake guildId, TrackScheduler trackScheduler) {
        this.client = client;
        this.guildId = guildId;
        this.trackScheduler = trackScheduler;

        this.listeners = new ConcurrentHashMap<>();
        this.isWaitingForChoice = new AtomicBoolean(false);
        this.isLeavingScheduled = new AtomicBoolean(false);
        this.messageChannelId = new AtomicLong();
        this.djId = new AtomicLong();
    }

    /**
     * Schedule to leave the voice channel in 1 minute
     */
    public void scheduleLeave() {
        LOGGER.debug("{Guild ID: {}} Scheduling auto-leave.", this.guildId.asLong());
        Mono.delay(Duration.ofMinutes(1), Schedulers.elastic())
                .filter(ignored -> this.isLeavingScheduled())
                .doOnNext(ignored -> MusicManager.getInstance().getConnection(this.guildId).leaveVoiceChannel())
                .doOnSubscribe(ignored -> this.isLeavingScheduled.set(true))
                .doOnTerminate(() -> this.isLeavingScheduled.set(false))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
    }

    public void cancelLeave() {
        LOGGER.debug("{Guild ID: {}} Cancelling auto-leave.", this.guildId.asLong());
        this.isLeavingScheduled.set(false);
    }

    public Mono<Void> end() {
        return Mono.fromRunnable(() -> {
            LOGGER.debug("{Guild ID: {}} Ending guild music.", this.guildId.asLong());
            MusicManager.getInstance().getConnection(this.guildId).leaveVoiceChannel();
        })
                .then(this.getMessageChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel))
                .then();
    }

    public DiscordClient getClient() {
        return this.client;
    }

    public TrackScheduler getTrackScheduler() {
        return this.trackScheduler;
    }

    public Snowflake getMessageChannelId() {
        return Snowflake.of(this.messageChannelId.get());
    }

    public Mono<MessageChannel> getMessageChannel() {
        return this.client.getChannelById(this.getMessageChannelId())
                .cast(MessageChannel.class);
    }

    public Snowflake getDjId() {
        return Snowflake.of(this.djId.get());
    }

    public boolean isWaitingForChoice() {
        return this.isWaitingForChoice.get();
    }

    public boolean isLeavingScheduled() {
        return this.isLeavingScheduled.get();
    }

    public void setMessageChannel(Snowflake messageChannelId) {
        this.messageChannelId.set(messageChannelId.asLong());
    }

    public void setDj(Snowflake djId) {
        this.djId.set(djId.asLong());
    }

    public void setWaitingForChoice(boolean isWaitingForChoice) {
        this.isWaitingForChoice.set(isWaitingForChoice);
    }

    public void addAudioLoadResultListener(AudioLoadResultListener listener, String identifier) {
        LOGGER.debug("{Guild ID: {}} Adding audio load result listener.", this.guildId.asLong());
        this.listeners.put(listener, MusicManager.getInstance().loadItemOrdered(this.guildId, identifier, listener));
    }

    public void removeAudioLoadResultListener(AudioLoadResultListener listener) {
        LOGGER.debug("{Guild ID: {}} Removing audio load result listener.", this.guildId.asLong());
        this.listeners.remove(listener);
        // If there is no music playing and nothing is loading, leave the voice channel
        if (this.trackScheduler.isStopped() && this.listeners.values().stream().allMatch(Future::isDone)) {
            MusicManager.getInstance().getConnection(this.guildId).leaveVoiceChannel();
        }
    }

    protected void destroy() {
        this.cancelLeave();
        this.listeners.values().forEach(task -> task.cancel(true));
        this.listeners.clear();
        this.trackScheduler.destroy();
    }

}
