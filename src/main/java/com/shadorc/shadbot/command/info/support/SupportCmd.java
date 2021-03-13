package com.shadorc.shadbot.command.info.support;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class SupportCmd extends BaseCmdGroup {

    public SupportCmd() {
        super(CommandCategory.INFO, "support", "Show support information",
                List.of(new FeedbackCmd(), new LinksCmd()));
    }

}
