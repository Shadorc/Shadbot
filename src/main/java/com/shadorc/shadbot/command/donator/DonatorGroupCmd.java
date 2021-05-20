package com.shadorc.shadbot.command.donator;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class DonatorGroupCmd extends GroupCmd {

    public DonatorGroupCmd() {
        super(CommandCategory.DONATOR, "Donator commands");
        this.addSubCommand(new ActivateRelicCmd(this));
        this.addSubCommand(new RelicStatusCmd(this));
    }

}
