package com.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import reactor.core.publisher.Mono;

class PauseCmd extends BaseCmd {

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
                        return context.reply(Emoji.PAUSE, context.localize("pause.paused"));
                    } else {
                        return context.reply(Emoji.PLAY, context.localize("pause.resumed"));
                    }
                });
    }

}
