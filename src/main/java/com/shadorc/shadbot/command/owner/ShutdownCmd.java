package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.ExitCode;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ShutdownCmd extends BaseCmd {

    public ShutdownCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("shutdown"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final boolean cleanShutdown = Boolean.parseBoolean(context.getArg().orElse("false"));
        return Shadbot.quit(cleanShutdown ? ExitCode.NORMAL_CLEAN : ExitCode.NORMAL);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Shutdown the bot.")
                .addArg("clean", "true if the logs should be cleaned on shutdown, false otherwise", true)
                .build();
    }

}
