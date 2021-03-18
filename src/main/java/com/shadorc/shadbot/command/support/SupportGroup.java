package com.shadorc.shadbot.command.support;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class SupportGroup extends BaseCmdGroup {

    public SupportGroup() {
        super(CommandCategory.INFO, "support", "Show support information",
                List.of(new FeedbackCmd(), new LinksCmd()));
    }

}
