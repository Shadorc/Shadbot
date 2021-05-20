package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.GroupCmd;

public class SettingGroupCmd extends GroupCmd {

    public SettingGroupCmd() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "Configure Shadbot");
        this.addSubCommand(new SettingShow(this));
        this.addSubCommand(new NSFWSetting(this));
        this.addSubCommand(new VolumeSetting(this));
        this.addSubCommand(new AutoMessagesSetting(this));
        this.addSubCommand(new AutoRolesSetting(this));
        this.addSubCommand(new AllowedRolesSetting(this));
        this.addSubCommand(new AllowedChannelsSetting(this));
        this.addSubCommand(new BlacklistSetting(this));
        this.addSubCommand(new RestrictedChannelsSetting(this));
        this.addSubCommand(new RestrictedRolesSetting(this));
        this.addSubCommand(new LocaleSetting(this));
        this.addSubCommand(new ResetSetting(this));
    }

}
