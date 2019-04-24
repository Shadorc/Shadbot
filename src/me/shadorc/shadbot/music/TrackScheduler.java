package me.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class TrackScheduler {

    public enum RepeatMode {
        NONE, SONG, PLAYLIST;
    }

    private final AudioPlayer audioPlayer;
    private final BlockingDeque<AudioTrack> queue;

    private RepeatMode repeatMode;
    private AudioTrack currentTrack;

    public TrackScheduler(AudioPlayer audioPlayer, int defaultVolume) {
        this.audioPlayer = audioPlayer;
        this.queue = new LinkedBlockingDeque<>();
        this.setRepeatMode(RepeatMode.NONE);
        this.setVolume(defaultVolume);
    }

    /**
     * @return {@code true} if the track was started, {@code false} if it was added to the queue
     */
    public boolean startOrQueue(AudioTrack track, boolean first) {
        StatsManager.VARIOUS_STATS.log(VariousEnum.MUSICS_LOADED);

        if (this.audioPlayer.startTrack(track.makeClone(), true)) {
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
     * @return {@code true} if the track was started, {@code false} otherwise
     */
    public boolean nextTrack() {
        switch (this.repeatMode) {
            case PLAYLIST:
                this.queue.offer(this.currentTrack.makeClone());
            case NONE:
                this.currentTrack = this.queue.poll();
                return this.audioPlayer.startTrack(this.currentTrack, false);
            case SONG:
                this.audioPlayer.playTrack(this.currentTrack.makeClone());
                break;
        }
        return true;
    }

    public void skipTo(int num) {
        AudioTrack track = null;
        for (int i = 0; i < num; i++) {
            track = this.queue.poll();
        }
        this.audioPlayer.playTrack(track.makeClone());
        this.currentTrack = track;
    }

    public long changePosition(long time) {
        final AudioTrack track = this.audioPlayer.getPlayingTrack();
        final long newPosition = NumberUtils.between(track.getPosition() + time, 0, track.getDuration() - 1);
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

    public void destroy() {
        this.audioPlayer.destroy();
        this.clearPlaylist();
    }

    public AudioPlayer getAudioPlayer() {
        return this.audioPlayer;
    }

    public BlockingQueue<AudioTrack> getPlaylist() {
        return this.queue;
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
        this.audioPlayer.setVolume(NumberUtils.between(volume, 0, 100));
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }
}