package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class InfoGroup extends BaseCmdGroup {

    public InfoGroup() {
        super(CommandCategory.INFO, "Show specific information",
                List.of(new BotInfoCmd(), new ServerInfoCmd(), new UserInfoCmd()));
    }

}
