package com.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import reactor.core.publisher.Mono;

public class NameCmd extends BaseCmd {

    public NameCmd() {
        super(CommandCategory.MUSIC, "name", "Show current music name");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        final AudioTrackInfo trackInfo = context.requireGuildMusic()
                .getTrackScheduler()
                .getAudioPlayer()
                .getPlayingTrack()
                .getInfo();

        return context.createFollowupMessage(Emoji.MUSICAL_NOTE + " (**%s**) Currently playing: **%s**",
                context.getAuthorName(), FormatUtil.trackName(trackInfo));
    }
}
