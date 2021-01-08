/*
package com.shadorc.shadbot.core.setting;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import reactor.core.publisher.Mono;

import java.util.List;

public abstract class BaseSetting extends BaseCmd {

    private final Setting setting;
    private final String description;

    protected BaseSetting(List<String> names, Setting setting, String description) {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, names, null);
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

    public abstract Mono<ImmutableEmbedFieldData> show(Context context, Settings settings);

}
*/
