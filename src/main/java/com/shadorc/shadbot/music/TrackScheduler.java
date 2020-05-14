package com.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.shadorc.shadbot.utils.NumberUtils;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class TrackScheduler {

    private static final float[] BASS_BOOST =
            {0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f};
    private static final float[] BASS_BOOST_2 =
            {-0.05f, 0.07f, 0.16f, 0.03f, -0.05f, -0.11f};

    public enum RepeatMode {
        NONE,
        SONG,
        PLAYLIST
    }

    private final AudioPlayer audioPlayer;
    private final BlockingDeque<AudioTrack> queue;
    private final EqualizerFactory equalizer;

    private RepeatMode repeatMode;
    private AudioTrack currentTrack;
    private int boostPercentage;

    public TrackScheduler(AudioPlayer audioPlayer, int defaultVolume) {
        this.audioPlayer = audioPlayer;
        this.queue = new LinkedBlockingDeque<>();
        this.equalizer = new EqualizerFactory();
        this.setRepeatMode(RepeatMode.NONE);
        this.setVolume(defaultVolume);
    }

    /**
     * @return {@code true} if the track was started, {@code false} if it was added to the queue.
     */
    public boolean startOrQueue(AudioTrack track, boolean first) {
        if (this.audioPlayer.startTrack(this.makeClone(track), true)) {
            this.currentTrack = track;
            return true;
        } else if (first) {
            this.queue.offerFirst(track);
            return false;
        } else {
            this.queue.offerLast(track);
            return false;
        }
    }

    /**
     * @return {@code true} if the track was started, {@code false} otherwise.
     */
    public boolean nextTrack() {
        switch (this.repeatMode) {
            case PLAYLIST:
                this.queue.offer(this.currentTrack);
            case NONE:
                this.currentTrack = this.queue.poll();
                return this.audioPlayer.startTrack(this.makeClone(this.currentTrack), false);
            case SONG:
                this.audioPlayer.playTrack(this.makeClone(this.currentTrack));
                break;
        }
        return true;
    }

    public void skipTo(int num) {
        AudioTrack track = null;
        for (int i = 0; i < num; i++) {
            track = this.queue.poll();
        }
        this.audioPlayer.playTrack(this.makeClone(track));
        this.currentTrack = track;
    }

    public long changePosition(long time) {
        final AudioTrack track = this.audioPlayer.getPlayingTrack();
        final long newPosition = NumberUtils.truncateBetween(track.getPosition() + time, 0, track.getDuration() - 1);
        track.setPosition(newPosition);
        return newPosition;
    }

    public void shufflePlaylist() {
        final List<AudioTrack> tempList = new ArrayList<>(this.queue);
        Collections.shuffle(tempList);
        this.queue.clear();
        this.queue.addAll(tempList);
    }

    public void clearPlaylist() {
        this.queue.clear();
    }

    public void boost(int percentage) {
        if (this.boostPercentage > 0 && percentage == 0) {
            this.audioPlayer.setFilterFactory(null);
            return;
        }
        if (this.boostPercentage == 0 && percentage > 0) {
            this.audioPlayer.setFilterFactory(this.equalizer);
        }

        final float multiplier = percentage / 100.0f;
        for (int i = 0; i < BASS_BOOST_2.length; i++) {
            this.equalizer.setGain(i, BASS_BOOST[i] * multiplier);
        }

        this.boostPercentage = percentage;
    }

    public void destroy() {
        if (this.currentTrack != null && this.currentTrack.getState() == AudioTrackState.PLAYING) {
            this.currentTrack.stop();
        }
        this.audioPlayer.destroy();
        this.clearPlaylist();
    }

    /**
     * @param track The {@link com.sedmelluq.discord.lavaplayer.track.AudioTrack} to clone.
     * @return A clone of the provided track or {@code null} if null input track.
     */
    @Nullable
    private AudioTrack makeClone(AudioTrack track) {
        return track == null ? null : track.makeClone();
    }

    public AudioPlayer getAudioPlayer() {
        return this.audioPlayer;
    }

    public Collection<AudioTrack> getPlaylist() {
        return Collections.unmodifiableCollection(this.queue);
    }

    public RepeatMode getRepeatMode() {
        return this.repeatMode;
    }

    public boolean isPlaying() {
        return this.audioPlayer.getPlayingTrack() != null;
    }

    public boolean isStopped() {
        return this.queue.isEmpty() && !this.isPlaying();
    }

    public void setVolume(int volume) {
        this.audioPlayer.setVolume(volume);
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }
}