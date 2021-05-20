package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;

public class CommandHelpBuilder extends HelpBuilder {

    private final String cmdName;

    public CommandHelpBuilder(Context context, BaseCmd cmd) {
        super(context);
        this.cmdName = cmd.getName();
        this.options.addAll(cmd.getOptions());

        this.setAuthor(context.localize("help.cmd.title").formatted(this.cmdName),
                "https://github.com/Shadorc/Shadbot/wiki/Commands");
        this.setDescription(cmd.getDescription());
    }

    @Override
    public String getCommandName() {
        return this.cmdName;
    }

}
