package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class OwnerGroupCmd extends GroupCmd {

    public OwnerGroupCmd() {
        super(CommandCategory.OWNER, "Owner commands");
        this.addSubCommand(new ShutdownCmd(this));
        this.addSubCommand(new EnableCommandCmd(this));
        this.addSubCommand(new LeaveGuildCmd(this));
        this.addSubCommand(new LoggerCmd(this));
        this.addSubCommand(new ManageAchievementsCmd(this));
        this.addSubCommand(new GenerateRelicCmd(this));
        this.addSubCommand(new SendMessageCmd(this));
    }

}
