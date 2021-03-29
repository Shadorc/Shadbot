package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;

import java.util.List;

public class SettingGroup extends BaseCmdGroup {

    public SettingGroup() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, "setting", "Configure Shadbot",
                List.of(new NSFWSetting(), new VolumeSetting(), new AutoMessagesSetting(), new AutoRolesSetting()));
    }

}
