package com.locibot.locibot.command.music;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

public class StopCmd extends BaseCmd {

    public StopCmd() {
        super(CommandCategory.MUSIC, "stop", "Stop current music");
    }

    @Override
    public Mono<?> execute(Context context) {
        context.requireGuildMusic();
        return context.getClient()
                .getVoiceConnectionRegistry()
                .getVoiceConnection(context.getGuildId())
                .flatMap(VoiceConnection::disconnect)
                .then(context.createFollowupMessage(Emoji.STOP_BUTTON, context.localize("stop.music")));
    }

}
