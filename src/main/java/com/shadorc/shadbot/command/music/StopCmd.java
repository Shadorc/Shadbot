package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

public class StopCmd extends Cmd {

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
