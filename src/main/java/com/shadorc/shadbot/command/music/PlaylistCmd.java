package com.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class PlaylistCmd extends BaseCmd {

    private static final int MAX_DESCRIPTION_LENGTH = 1800;

    public PlaylistCmd() {
        super(CommandCategory.MUSIC, "playlist", "Current playlist");
    }

    @Override
    public Mono<?> execute(Context context) {
        final GuildMusic guildMusic = context.requireGuildMusic();

        return context.reply(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("playlist.title"), null, context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/IG3Hj2W.png")
                        .setDescription(PlaylistCmd.formatPlaylist(context, guildMusic.getTrackScheduler()))));
    }

    private static String formatPlaylist(Context context, TrackScheduler trackScheduler) {
        final AudioTrack currentTrack = trackScheduler.getAudioPlayer().getPlayingTrack();
        final int musicCount = trackScheduler.getPlaylist().size() + (currentTrack == null ? 0 : 1);
        if (musicCount == 0) {
            return context.localize("playlist.empty");
        }

        final StringBuilder playlistStr = new StringBuilder(context.localize("playlist.music.count")
                .formatted(musicCount));

        playlistStr.append("%n\t**1.** [%s](%s)"
                .formatted(FormatUtil.trackName(context.getLocale(), currentTrack.getInfo()), currentTrack.getInfo().uri));

        int count = 2;
        for (final AudioTrack track : trackScheduler.getPlaylist()) {
            final String name = "%n\t**%d.** [%s](%s)"
                    .formatted(count, FormatUtil.trackName(context.getLocale(), track.getInfo()), track.getInfo().uri);
            if (playlistStr.length() + name.length() < MAX_DESCRIPTION_LENGTH) {
                playlistStr.append(name);
            } else {
                playlistStr.append("\n\t...");
                break;
            }
            count++;
        }
        return playlistStr.toString();
    }

}
