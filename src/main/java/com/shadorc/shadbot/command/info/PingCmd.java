/*
package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Mono;

public class PingCmd extends BaseCmd {

    public PingCmd() {
        super(CommandCategory.INFO, "ping", "Show bot's ping");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        final long start = System.currentTimeMillis();
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.GEAR + " (**%s**) Testing ping...", context.getAuthorName()), channel))
                .flatMap(message -> message.edit(spec -> spec.setContent(
                        String.format(Emoji.GEAR + " Ping: %dms", TimeUtils.getMillisUntil(start)))));
    }

}
*/
