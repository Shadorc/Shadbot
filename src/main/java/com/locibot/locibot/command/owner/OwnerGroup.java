package com.locibot.locibot.command.owner;

import com.locibot.locibot.command.owner.shutdown.ShutdownCmd;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class OwnerGroup extends BaseCmdGroup {

    public OwnerGroup() {
        super(CommandCategory.OWNER, "owner", "Owner commands",
                List.of(new ShutdownCmd(), new EnableCommandCmd(), new LeaveGuildCmd(), new LoggerCmd(),
                        new ManageAchievementsCmd(), new GenerateRelicCmd(), new SendMessageCmd()));
    }

}
