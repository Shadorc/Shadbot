package com.locibot.locibot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import reactor.core.publisher.Mono;

public class PauseCmd extends BaseCmd {

    public PauseCmd() {
        super(CommandCategory.MUSIC, "pause", "Toggle pause for current music");
    }

    @Override
    public Mono<?> execute(Context context) {
        final AudioPlayer audioPlayer = context.requireGuildMusic().getTrackScheduler().getAudioPlayer();

        return DiscordUtil.requireVoiceChannel(context)
                .flatMap(__ -> {
                    audioPlayer.setPaused(!audioPlayer.isPaused());
                    if (audioPlayer.isPaused()) {
                        return context.createFollowupMessage(Emoji.PAUSE, context.localize("pause.paused"));
                    } else {
                        return context.createFollowupMessage(Emoji.PLAY, context.localize("pause.resumed"));
                    }
                });
    }

}
