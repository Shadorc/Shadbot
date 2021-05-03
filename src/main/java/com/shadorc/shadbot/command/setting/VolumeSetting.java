package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class VolumeSetting extends BaseCmd {

    private static final int MIN_VOLUME = 1;
    private static final int MAX_VOLUME = 75;

    public VolumeSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "default_volume", "Manage music's default volume");

        this.addOption(option -> option.name("volume")
                .description("New default volume (min:%d / max:%d / default:%d)"
                        .formatted(MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME))
                .required(true)
                .type(ApplicationCommandOptionType.INTEGER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final long volume = context.getOptionAsLong("volume").orElseThrow();

        if (!NumberUtil.isBetween(volume, MIN_VOLUME, MAX_VOLUME)) {
            return Mono.error(new CommandException(context.localize("setting.volume.invalid")
                    .formatted(MIN_VOLUME, MAX_VOLUME)));
        }

        return context.getDbGuild()
                .updateSetting(Setting.DEFAULT_VOLUME, volume)
                .then(context.createFollowupMessage(Emoji.CHECK_MARK,
                        context.localize("setting.volume.message").formatted(volume)));
    }
}
