/*
package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class EnableCommandCmd extends BaseCmd {

    public EnableCommandCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("enable_command"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final BaseCmd cmd = CommandManager.getInstance().getCommand(args.get(0));
        if (cmd == null) {
            throw new CommandException(String.format("Command `%s` not found.", args.get(0)));
        }

        if (!"true".equalsIgnoreCase(args.get(1)) && !"false".equalsIgnoreCase(args.get(1))) {
            throw new CommandException(String.format("`%s` is not a correct value for a boolean.", args.get(1)));
        }

        final boolean enabled = Boolean.parseBoolean(args.get(1));
        cmd.setEnabled(enabled);

        DEFAULT_LOGGER.info("Command {} {}", cmd.getName(), enabled ? "enabled" : "disabled");

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command `%s` %s",
                        cmd.getName(), enabled ? "enabled" : "disabled"), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Enable/disable a command.")
                .addArg("command", false)
                .addArg("enabled", "true/false", false)
                .build();
    }
}
*/
