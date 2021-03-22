package com.shadorc.shadbot.music;

import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.voice.VoiceConnection;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class GuildMusic {

    private static final Duration LEAVE_DELAY = Duration.ofMinutes(1);

    private final GatewayDiscordClient gateway;
    private final long guildId;
    private final TrackScheduler trackScheduler;

    private final Map<AudioLoadResultListener, Future<Void>> listeners;
    private final AtomicBoolean isWaitingForChoice;
    private final AtomicLong messageChannelId;
    private final AtomicLong djId;
    private final AtomicReference<Disposable> leavingTask;

    public GuildMusic(GatewayDiscordClient gateway, Snowflake guildId, TrackScheduler trackScheduler) {
        this.gateway = gateway;
        this.guildId = guildId.asLong();
        this.trackScheduler = trackScheduler;

        this.listeners = new ConcurrentHashMap<>();
        this.isWaitingForChoice = new AtomicBoolean(false);
        this.messageChannelId = new AtomicLong();
        this.djId = new AtomicLong();
        this.leavingTask = new AtomicReference<>();
    }

    /**
     * Schedules to leave the voice channel in 1 minute.
     */
    public void scheduleLeave() {
        LOGGER.debug("{Guild ID: {}} Scheduling auto-leave", this.guildId);
        this.leavingTask.set(Mono.delay(LEAVE_DELAY, Schedulers.boundedElastic())
                .filter(__ -> this.isLeavingScheduled())
                .map(__ -> this.gateway.getVoiceConnectionRegistry())
                .flatMap(registry -> registry.getVoiceConnection(this.getGuildId()))
                .flatMap(VoiceConnection::disconnect)
                .subscribe(null, ExceptionHandler::handleUnknownError));
    }

    public void cancelLeave() {
        if (this.isLeavingScheduled()) {
            LOGGER.debug("{Guild ID: {}} Cancelling auto-leave", this.guildId);
            this.leavingTask.get().dispose();
        }
    }

    public void addAudioLoadResultListener(AudioLoadResultListener listener) {
        LOGGER.debug("{Guild ID: {}} Adding audio load result listener: {}", this.guildId, listener.hashCode());
        this.listeners.put(listener, MusicManager.loadItemOrdered(this.guildId, listener));
    }

    public void removeAudioLoadResultListener(AudioLoadResultListener listener) {
        LOGGER.debug("{Guild ID: {}} Removing audio load result listener: {}", this.guildId, listener.hashCode());
        this.listeners.remove(listener);
    }

    public Mono<Message> end() {
        LOGGER.debug("{Guild ID: {}} Ending guild music", this.guildId);
        return this.getGateway()
                .getVoiceConnectionRegistry()
                .getVoiceConnection(this.getGuildId())
                .flatMap(VoiceConnection::disconnect)
                .then(this.getMessageChannel())
                .zipWith(DatabaseManager.getGuilds().getDBGuild(this.getGuildId())
                        .map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((channel, locale) -> DiscordUtil.sendMessage(Emoji.INFO,
                        I18nManager.localize(locale, "music.playlist.end"),
                        channel)));
    }

    public GatewayDiscordClient getGateway() {
        return this.gateway;
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public TrackScheduler getTrackScheduler() {
        return this.trackScheduler;
    }

    public Snowflake getMessageChannelId() {
        return Snowflake.of(this.messageChannelId.get());
    }

    public Mono<MessageChannel> getMessageChannel() {
        return this.gateway.getChannelById(this.getMessageChannelId())
                .cast(MessageChannel.class);
    }

    public Snowflake getDjId() {
        return Snowflake.of(this.djId.get());
    }

    public Mono<User> getDj() {
        return this.gateway.getUserById(this.getDjId());
    }

    public boolean isWaitingForChoice() {
        return this.isWaitingForChoice.get();
    }

    public boolean isLeavingScheduled() {
        return this.leavingTask.get() != null && !this.leavingTask.get().isDisposed();
    }

    public boolean isWaitingForListeners() {
        return !this.listeners.values().stream().allMatch(Future::isDone);
    }

    public void setWaitingForChoice(boolean isWaitingForChoice) {
        this.isWaitingForChoice.set(isWaitingForChoice);
    }

    public void setMessageChannelId(Snowflake messageChannelId) {
        this.messageChannelId.set(messageChannelId.asLong());
    }

    public void setDjId(Snowflake djId) {
        this.djId.set(djId.asLong());
    }

    public void destroy() {
        this.cancelLeave();
        this.listeners.values().forEach(task -> task.cancel(true));
        this.listeners.clear();
        this.trackScheduler.destroy();
    }
}
