package com.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import reactor.core.publisher.Mono;

public class PauseCmd extends BaseCmd {

    public PauseCmd() {
        super(CommandCategory.MUSIC, "pause", "Toggle pause for current music");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        final AudioPlayer audioPlayer = context.requireGuildMusic().getTrackScheduler().getAudioPlayer();

        return DiscordUtil.requireVoiceChannel(context)
                .map(__ -> {
                    audioPlayer.setPaused(!audioPlayer.isPaused());
                    if (audioPlayer.isPaused()) {
                        return String.format(Emoji.PAUSE + " Music paused by **%s**.", context.getAuthorName());
                    } else {
                        return String.format(Emoji.PLAY + " Music resumed by **%s**.", context.getAuthorName());
                    }
                })
                .flatMap(context::createFollowupMessage);
    }

}
