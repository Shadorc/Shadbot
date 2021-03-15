package com.shadorc.shadbot.command.todo;

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
        final String message = "%s (**%s**) %s"
                .formatted(Emoji.GEAR, context.getAuthorName(), context.localize("testing.ping"));
        return context.createFollowupMessage(message)
                .flatMap(messageId -> context.editReply(messageId,
                        "%s Ping: %sms", Emoji.GEAR, context.localize(TimeUtil.elapsed(start))));
    }

}
