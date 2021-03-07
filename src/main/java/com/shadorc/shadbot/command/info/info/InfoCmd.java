package com.shadorc.shadbot.command.info.info;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class InfoCmd extends BaseCmdGroup {

    public InfoCmd() {
        super(CommandCategory.INFO, "info", "Show specific information",
                List.of(new BotInfoCmd(), new ServerInfoCmd(), new UserInfoCmd()));
    }

}
