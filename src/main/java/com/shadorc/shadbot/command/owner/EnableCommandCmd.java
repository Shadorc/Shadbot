package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

class EnableCommandCmd extends BaseCmd {

    public EnableCommandCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "enable_command", "Enable/disable a command");
        this.addOption("command", "The command to enable/disable", true,
                ApplicationCommandOptionType.STRING);
        this.addOption("enabled", "True to enable, false to disable", true,
                ApplicationCommandOptionType.BOOLEAN);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String commandName = context.getOptionAsString("command").orElseThrow();
        final BaseCmd cmd = CommandManager.getInstance().getCommand(commandName);
        if (cmd == null) {
            return Mono.error(new CommandException("Command `%s` not found.".formatted(commandName)));
        }

        final boolean enabled = context.getOptionAsBool("enabled").orElseThrow();
        cmd.setEnabled(enabled);

        final String enabledStr = enabled ? "enabled" : "disabled";
        DEFAULT_LOGGER.info("Command {} {}", cmd.getName(), enabledStr);

        return context.reply(Emoji.CHECK_MARK, "Command `%s` %s".formatted(commandName, enabledStr));
    }

}
