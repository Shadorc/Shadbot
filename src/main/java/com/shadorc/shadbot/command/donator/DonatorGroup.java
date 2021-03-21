package com.shadorc.shadbot.command.donator;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class DonatorGroup extends BaseCmdGroup {

    public DonatorGroup() {
        super(CommandCategory.DONATOR, "donator", "Donator commands",
                List.of(new ActivateRelicCmd(), new RelicStatusCmd()));
    }

}
