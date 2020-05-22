package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.function.Consumer;

public class SettingHelpBuilder extends HelpBuilder {

    private final BaseSetting setting;

    private SettingHelpBuilder(BaseSetting setting, Context context) {
        super(context);
        this.setting = setting;
    }

    public static SettingHelpBuilder create(BaseSetting setting, Context context) {
        return new SettingHelpBuilder(setting, context);
    }

    @Override
    protected String getCommandName() {
        return this.setting.getCommandName();
    }

    @Override
    public Consumer<EmbedCreateSpec> build() {
        this.setAuthor(String.format("Help for setting: %s", this.setting.getName()),
                "https://github.com/Shadorc/Shadbot/wiki/Settings")
                .setDescription(String.format("**%s**", this.setting.getDescription()));

        return super.build();
    }
}
