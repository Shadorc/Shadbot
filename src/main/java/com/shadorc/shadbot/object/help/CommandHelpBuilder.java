package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;

public class CommandHelpBuilder extends HelpBuilder {

    private final BaseCmd cmd;

    private CommandHelpBuilder(BaseCmd cmd, Context context) {
        super(context);
        this.cmd = cmd;
        this.options.addAll(cmd.getOptions());

        this.setAuthor(String.format("Help for command: %s", this.cmd.getName()),
                "https://github.com/Shadorc/Shadbot/wiki/Commands");
        this.setDescription(cmd.getDescription());
    }

    public static CommandHelpBuilder create(BaseCmd cmd, Context context) {
        return new CommandHelpBuilder(cmd, context);
    }

    public CommandHelpBuilder setUsage(String usage) {
        this.setFullUsage(String.format("/%s %s", this.cmd.getName(), usage));
        return this;
    }

    @Override
    public String getCommandName() {
        return this.cmd.getName();
    }

}
