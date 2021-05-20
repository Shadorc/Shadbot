package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class InfoGroupCmd extends GroupCmd {

    public InfoGroupCmd() {
        super(CommandCategory.INFO, "Show specific information");
        this.addSubCommand(new BotInfoCmd(this));
        this.addSubCommand(new ServerInfoCmd(this));
        this.addSubCommand(new UserInfoCmd(this));
    }

}
