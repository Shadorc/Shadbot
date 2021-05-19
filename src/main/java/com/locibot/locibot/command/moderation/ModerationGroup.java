package com.locibot.locibot.command.moderation;

import com.locibot.locibot.command.moderation.member.BanCmd;
import com.locibot.locibot.command.moderation.member.KickCmd;
import com.locibot.locibot.command.moderation.member.SoftBanCmd;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class ModerationGroup extends BaseCmdGroup {

    public ModerationGroup() {
        super(CommandCategory.MODERATION, "moderation", "Manages your server",
                List.of(new RolelistCmd(), new PruneCmd(), new KickCmd(), new BanCmd(), new SoftBanCmd(),
                        new ManageCoinsCmd(), new IamCmd()));
    }

}
