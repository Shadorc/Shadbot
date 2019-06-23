package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class PingCmd extends BaseCmd {

    public PingCmd() {
        super(CommandCategory.INFO, List.of("ping"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final long start = System.currentTimeMillis();
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GEAR + " (**%s**) Testing ping...", context.getUsername()), channel))
                .flatMap(message -> message.edit(spec -> spec.setContent(String.format(Emoji.GEAR + " Ping: %dms", TimeUtils.getMillisUntil(start)))))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show Shadbot's ping.")
                .build();
    }
}
