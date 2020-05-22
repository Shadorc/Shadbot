package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class VolumeSetting extends BaseSetting {

    private static final int MIN_VOLUME = 1;
    private static final int MAX_VOLUME = 75;

    public VolumeSetting() {
        super(List.of("default_volume", "volume"),
                Setting.DEFAULT_VOLUME, "Manage music's default volume.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Integer volume = NumberUtils.toIntBetweenOrNull(args.get(1), MIN_VOLUME, MAX_VOLUME);
        if (volume == null) {
            return Mono.error(new CommandException(
                    String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
                            args.get(1), MIN_VOLUME, MAX_VOLUME)));
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> dbGuild.setSetting(this.getSetting(), volume))
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " Default volume set to **%d%%**", volume), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("volume", "new default volume", false)
                .setExample(String.format("`%s%s 42`", context.getPrefix(), this.getCommandName()))
                .addField("Restrictions", String.format("min: %d / max: %d / default: %d",
                        MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME), false)
                .build();
    }

}
