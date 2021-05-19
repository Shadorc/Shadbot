package com.locibot.locibot.command.info;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class InfoGroup extends BaseCmdGroup {

    public InfoGroup() {
        super(CommandCategory.INFO, "info", "Show specific information",
                List.of(new BotInfoCmd(), new ServerInfoCmd(), new UserInfoCmd()));
    }

}
