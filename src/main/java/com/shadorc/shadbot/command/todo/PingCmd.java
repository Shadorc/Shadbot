package com.shadorc.shadbot.command.todo;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.TimeUtil;
import reactor.core.publisher.Mono;

public class PingCmd extends BaseCmd {

    public PingCmd() {
        super(CommandCategory.INFO, "ping", "Show current ping");
    }

    @Override
    public Mono<?> execute(Context context) {
        final long start = System.currentTimeMillis();
        final String message = "%s (**%s**) %s"
                .formatted(Emoji.GEAR, context.getAuthorName(), context.translate("testing.ping"));
        return context.createFollowupMessage(message)
                .flatMap(messageId -> context.editFollowupMessage(messageId,
                        "%s Ping: %dms", Emoji.GEAR, TimeUtil.getMillisUntil(start)));
    }

}
