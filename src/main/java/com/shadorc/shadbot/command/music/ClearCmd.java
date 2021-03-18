package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import reactor.core.publisher.Mono;

public class ClearCmd extends BaseCmd {

    public ClearCmd() {
        super(CommandCategory.MUSIC, "clear", "Clear current playlist");
    }

    @Override
    public Mono<?> execute(Context context) {
        context.requireGuildMusic().getTrackScheduler().clearPlaylist();
        return context.reply(Emoji.CHECK_MARK, context.localize("clear.message"));
    }

}
