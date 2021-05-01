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
        super(CommandCategory.MUSIC, "name", "Current music name");
    }

    @Override
    public Mono<?> execute(Context context) {
        final AudioTrackInfo trackInfo = context.requireGuildMusic()
                .getTrackScheduler()
                .getAudioPlayer()
                .getPlayingTrack()
                .getInfo();

        return context.reply(Emoji.MUSICAL_NOTE, context.localize("name.message")
                .formatted(FormatUtil.trackName(context.getLocale(), trackInfo)));
    }
}
