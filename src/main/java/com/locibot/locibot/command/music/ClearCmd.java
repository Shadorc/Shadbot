package com.locibot.locibot.command.music;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import reactor.core.publisher.Mono;

public class ClearCmd extends BaseCmd {

    public ClearCmd() {
        super(CommandCategory.MUSIC, "clear", "Clear current playlist");
    }

    @Override
    public Mono<?> execute(Context context) {
        context.requireGuildMusic().getTrackScheduler().clearPlaylist();
        return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("clear.message"));
    }

}
