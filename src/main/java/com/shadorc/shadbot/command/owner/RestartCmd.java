package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExitCode;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class RestartCmd extends BaseCmd {

    public RestartCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("restart"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.QUESTION + " (**%s**) Do you really want to restart ? y/n", context.getUsername()), channel))
                .then(Mono.fromRunnable(() -> new ConfirmInputs(context.getClient(), Duration.ofSeconds(15),
                        Shadbot.quit(ExitCode.RESTART))
                        .subscribe()));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Restart the bot.")
                .build();
    }

}
