package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class VolumeSetting extends BaseCmd {

    private static final int MIN_VOLUME = 1;
    private static final int MAX_VOLUME = 75;

    private final Setting setting;

    public VolumeSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "default_volume", "Manage music's default volume.");
        this.setting = Setting.DEFAULT_VOLUME;

        this.addOption("volume", "New default volume (min:%d / max:%d / default:%d)"
                        .formatted(MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME), true,
                ApplicationCommandOptionType.INTEGER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String volumeStr = context.getOptionAsString("volume").orElseThrow();

        final Integer volume = NumberUtil.toIntBetweenOrNull(volumeStr, MIN_VOLUME, MAX_VOLUME);
        if (volume == null) {
            return Mono.error(new CommandException(context.localize("setting.volume.invalid")
                    .formatted(volumeStr, MIN_VOLUME, MAX_VOLUME)));
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> dbGuild.updateSetting(this.setting, volume))
                .then(context.getChannel())
                .flatMap(channel -> context.reply(Emoji.CHECK_MARK,
                        context.localize("setting.volume.message").formatted(volume)));
    }
}
