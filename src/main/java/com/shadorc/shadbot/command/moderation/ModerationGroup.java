package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.command.moderation.member.BanCmd;
import com.shadorc.shadbot.command.moderation.member.KickCmd;
import com.shadorc.shadbot.command.moderation.member.SoftBanCmd;
import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class ModerationGroup extends BaseCmdGroup {

    public ModerationGroup() {
        super(CommandCategory.MODERATION, "moderation", "Manages your server",
                List.of(new RolelistCmd(), new PruneCmd(), new KickCmd(), new BanCmd(), new SoftBanCmd(),
                        new ManageCoinsCmd(), new IamCmd()));
    }

}
