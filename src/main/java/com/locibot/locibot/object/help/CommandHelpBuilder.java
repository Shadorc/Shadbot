package com.locibot.locibot.object.help;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.Context;

public class CommandHelpBuilder extends HelpBuilder {

    private final BaseCmd cmd;

    private CommandHelpBuilder(Context context, BaseCmd cmd) {
        super(context);
        this.cmd = cmd;
        this.options.addAll(cmd.getOptions());

        this.setAuthor(context.localize("help.cmd.title").formatted(this.cmd.getName()),
                "https://github.com/Shadorc/Shadbot/wiki/Commands");
        this.setDescription(cmd.getDescription());
    }

    public static CommandHelpBuilder create(Context context, BaseCmd cmd) {
        return new CommandHelpBuilder(context, cmd);
    }

    @Override
    public String getCommandName() {
        return this.cmd.getName();
    }

}
