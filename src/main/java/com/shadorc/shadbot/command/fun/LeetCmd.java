package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class LeetCmd extends BaseCmd {

    private static final int MAX_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;

    public LeetCmd() {
        super(CommandCategory.FUN, List.of("leet"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final String text = arg.toUpperCase()
                .replace("A", "4")
                .replace("B", "8")
                .replace("E", "3")
                .replace("G", "6")
                .replace("L", "1")
                .replace("O", "0")
                .replace("S", "5")
                .replace("T", "7");

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor("Leetifier", null, context.getAvatarUrl())
                        .setDescription(String.format("**Original**%n%s%n%n**Leetified**%n%s",
                                StringUtils.abbreviate(arg, MAX_LENGTH), StringUtils.abbreviate(text, MAX_LENGTH))));

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Leetify a text.")
                .addArg("text", false)
                .build();
    }

}
