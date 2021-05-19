package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.TimeUtil;
import reactor.core.publisher.Mono;

public class PingCmd extends BaseCmd {

    public PingCmd() {
        super(CommandCategory.INFO, "ping", "Test ping");
    }

    @Override
    public Mono<?> execute(Context context) {
        final long start = System.currentTimeMillis();
        return context.createFollowupMessage(Emoji.GEAR, context.localize("testing.ping"))
                .flatMap(messageId -> context.editFollowupMessage(Emoji.GEAR, context.localize("ping.message")
                        .formatted(context.localize(TimeUtil.elapsed(start).toMillis()))));
    }

}
