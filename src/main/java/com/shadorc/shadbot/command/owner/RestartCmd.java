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

public class RestartCmd extends BaseCmd {

    public RestartCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("restart"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final boolean cleanRestart = Boolean.valueOf(context.getArg().orElse("false"));
        return Shadbot.quit(cleanRestart ? ExitCode.RESTART_CLEAN : ExitCode.RESTART);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Restart the bot.")
                .addArg("clean", "true if the logs should be cleaned on restart, false otherwise", true)
                .build();
    }

}
