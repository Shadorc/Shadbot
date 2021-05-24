package com.locibot.locibot.command.group;

import com.locibot.locibot.command.group.amongUs.AmongUs;
import com.locibot.locibot.command.group.clash.Clash;
import com.locibot.locibot.command.info.InfoGroupCmd;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

public class GroupGroup extends BaseCmdGroup {
    public GroupGroup() {
        super(CommandCategory.GROUP, CommandPermission.USER_GUILD, "group", "Group Commands",
                List.of(
                        //core
                        new Schedule(), new Delete(),
                        //Groups
                        new Clash(), new AmongUs()));
    }
}
