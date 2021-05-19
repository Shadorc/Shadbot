package com.locibot.locibot.command.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
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

        return context.createFollowupMessage(Emoji.MUSICAL_NOTE, context.localize("name.message")
                .formatted(FormatUtil.trackName(context.getLocale(), trackInfo)));
    }
}
