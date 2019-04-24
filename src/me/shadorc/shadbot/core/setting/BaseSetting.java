package me.shadorc.shadbot.core.setting;

import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;

import java.util.List;

public abstract class BaseSetting extends BaseCmd {

    private final Setting setting;
    private final String description;

    public BaseSetting(Setting setting, String description) {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of(setting.toString()), null);
        this.setting = setting;
        this.description = description;
    }

    public Setting getSetting() {
        return this.setting;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCommandName() {
        return String.format("setting %s", this.getName());
    }

}
