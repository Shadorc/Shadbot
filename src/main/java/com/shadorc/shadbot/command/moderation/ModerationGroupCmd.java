package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.command.moderation.member.BanCmd;
import com.shadorc.shadbot.command.moderation.member.KickCmd;
import com.shadorc.shadbot.command.moderation.member.SoftBanCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class ModerationGroupCmd extends GroupCmd {

    public ModerationGroupCmd() {
        super(CommandCategory.MODERATION, "Manages your server");
        this.addSubCommand(new RolelistCmd(this));
        this.addSubCommand(new PruneCmd(this));
        this.addSubCommand(new KickCmd(this));
        this.addSubCommand(new BanCmd(this));
        this.addSubCommand(new SoftBanCmd(this));
        this.addSubCommand(new ManageCoinsCmd(this));
        this.addSubCommand(new IamCmd(this));
    }
}
