package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

public class StopCmd extends BaseCmd {

    public StopCmd() {
        super(CommandCategory.MUSIC, "stop", "Stop current music");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        context.requireGuildMusic();
        return context.getClient()
                .getVoiceConnectionRegistry()
                .getVoiceConnection(context.getGuildId())
                .flatMap(VoiceConnection::disconnect)
                .then(context.createFollowupMessage(Emoji.STOP_BUTTON + " Music stopped by **%s**.", context.getAuthorName()));
    }

}
