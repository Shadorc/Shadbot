package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.owner.shutdown.ShutdownCmd;
import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class OwnerGroup extends BaseCmdGroup {

    public OwnerGroup() {
        super(CommandCategory.OWNER, "owner", "Owner commands",
                List.of(new ShutdownCmd(), new EnableCommandCmd(), new LeaveGuildCmd(), new LoggerCmd(),
                        new ManageAchievementsCmd(), new GenerateRelicCmd(), new SendMessageCmd()));
    }

}
