package me.shadorc.shadbot.command.info;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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
