package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.function.Consumer;

public class CommandHelpBuilder extends HelpBuilder {

    private final BaseCmd cmd;

    private CommandHelpBuilder(BaseCmd cmd, Context context) {
        super(context);
        this.cmd = cmd;
    }

    public static CommandHelpBuilder create(BaseCmd cmd, Context context) {
        return new CommandHelpBuilder(cmd, context);
    }

    public CommandHelpBuilder setDescription(String description) {
        super.setDescription(description);
        return this;
    }

    public CommandHelpBuilder setUsage(String usage) {
        this.setFullUsage(String.format("/%s %s", this.cmd.getName(), usage));
        return this;
    }

    @Override
    public String getCommandName() {
        return this.cmd.getName();
    }

    @Override
    public Consumer<EmbedCreateSpec> build() {
        this.setAuthor(String.format("Help for command: %s", this.cmd.getName()),
                "https://github.com/Shadorc/Shadbot/wiki/Commands");
      /*  this.cmd.getAlias()
                .filter(alias -> !alias.isBlank())
                .ifPresent(alias -> this.setFooter(String.format("Alias: %s", alias)));*/

        return super.build();
    }

}
