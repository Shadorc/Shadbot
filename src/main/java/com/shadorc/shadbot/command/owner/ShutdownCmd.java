package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class ShutdownCmd extends BaseCmd {

    public ShutdownCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("shutdown"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.QUESTION + " (**%s**) Do you really want to shutdown ? y/n",
                                context.getUsername()), channel))
                .then(Mono.fromRunnable(() ->
                        ConfirmInputs.create(context.getClient(), Duration.ofSeconds(15), context.getChannelId(), Shadbot.quit()).listen()));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Shutdown the bot.")
                .build();
    }

}
