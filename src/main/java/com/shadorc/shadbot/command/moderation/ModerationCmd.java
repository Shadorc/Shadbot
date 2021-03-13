package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class ModerationCmd extends BaseCmdGroup {

    public ModerationCmd() {
        super(CommandCategory.MODERATION, "moderation", "Manages your server",
                List.of(new RolelistCmd()));
    }

}
