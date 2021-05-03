package com.shadorc.shadbot.command.standalone;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.TimeUtil;
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
