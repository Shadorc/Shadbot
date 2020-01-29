package com.shadorc.shadbot.music;

import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class GuildMusic {

    private final GatewayDiscordClient client;
    private final Snowflake guildId;
    private final TrackScheduler trackScheduler;
    private final Map<AudioLoadResultListener, Future<Void>> listeners;

    private boolean isWaitingForChoice;
    private Snowflake messageChannelId;
    private Snowflake djId;
    private Disposable leavingTask;

    public GuildMusic(GatewayDiscordClient client, Snowflake guildId, TrackScheduler trackScheduler) {
        this.client = client;
        this.guildId = guildId;
        this.trackScheduler = trackScheduler;
        this.listeners = new ConcurrentHashMap<>();
        this.isWaitingForChoice = false;
    }

    /**
     * Schedule to leave the voice channel in 1 minute.
     */
    public void scheduleLeave() {
        LOGGER.debug("{Guild ID: {}} Scheduling auto-leave.", this.guildId.asLong());
        this.leavingTask = Mono.delay(Duration.ofMinutes(1), Schedulers.boundedElastic())
                .filter(ignored -> this.isLeavingScheduled())
                .flatMap(ignored -> MusicManager.getInstance().getConnection(this.guildId).leaveVoiceChannel())
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    public void cancelLeave() {
        if (this.isLeavingScheduled()) {
            LOGGER.debug("{Guild ID: {}} Cancelling auto-leave.", this.guildId.asLong());
            this.leavingTask.dispose();
        }
    }

    public Mono<Void> end() {
        LOGGER.debug("{Guild ID: {}} Ending guild music.", this.guildId.asLong());
        return MusicManager.getInstance()
                .getConnection(this.guildId)
                .leaveVoiceChannel()
                .then(this.getMessageChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel))
                .then();
    }

    public GatewayDiscordClient getClient() {
        return this.client;
    }

    public TrackScheduler getTrackScheduler() {
        return this.trackScheduler;
    }

    public Snowflake getMessageChannelId() {
        return this.messageChannelId;
    }

    public Mono<MessageChannel> getMessageChannel() {
        return this.client.getChannelById(this.getMessageChannelId())
                .cast(MessageChannel.class);
    }

    public Snowflake getDjId() {
        return this.djId;
    }

    public boolean isWaitingForChoice() {
        return this.isWaitingForChoice;
    }

    public boolean isLeavingScheduled() {
        return this.leavingTask != null && !this.leavingTask.isDisposed();
    }

    public void setMessageChannel(Snowflake messageChannelId) {
        this.messageChannelId = messageChannelId;
    }

    public void setDj(Snowflake djId) {
        this.djId = djId;
    }

    public void setWaitingForChoice(boolean isWaitingForChoice) {
        this.isWaitingForChoice = isWaitingForChoice;
    }

    public void addAudioLoadResultListener(AudioLoadResultListener listener, String identifier) {
        LOGGER.debug("{Guild ID: {}} Adding audio load result listener: {}", this.guildId.asLong(), listener);
        this.listeners.put(listener, MusicManager.getInstance().loadItemOrdered(this.guildId, identifier, listener));
    }

    public Mono<Void> removeAudioLoadResultListener(AudioLoadResultListener listener) {
        LOGGER.debug("{Guild ID: {}} Removing audio load result listener: {}", this.guildId.asLong(), listener);
        this.listeners.remove(listener);
        // If there is no music playing and nothing is loading, leave the voice channel
        if (this.trackScheduler.isStopped() && this.listeners.values().stream().allMatch(Future::isDone)) {
            return MusicManager.getInstance()
                    .getConnection(this.guildId)
                    .leaveVoiceChannel();
        }
        return Mono.empty();
    }

    protected void destroy() {
        this.cancelLeave();
        this.listeners.values().forEach(task -> task.cancel(true));
        this.listeners.clear();
        this.trackScheduler.destroy();
    }

}
