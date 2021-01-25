package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.TimeUtil;
import reactor.core.publisher.Mono;

public class PingCmd extends BaseCmd {

    public PingCmd() {
        super(CommandCategory.INFO, "ping", "Show bot's ping");
    }

    @Override
    public Mono<?> execute(Context context) {
        final long start = System.currentTimeMillis();
        return context.createFollowupMessage(Emoji.GEAR + " (**%s**) Testing ping...", context.getAuthorName())
                .flatMap(messageId -> context.editFollowupMessage(messageId,
                        Emoji.GEAR + " Ping: %dms", TimeUtil.getMillisUntil(start)));
    }

}
