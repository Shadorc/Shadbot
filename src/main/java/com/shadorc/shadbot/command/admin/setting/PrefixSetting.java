package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class PrefixSetting extends BaseSetting {

    private static final int MAX_PREFIX_LENGTH = 5;

    public PrefixSetting() {
        super(Setting.PREFIX, "Manage Shadbot's prefix.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        if (args.get(1).length() > MAX_PREFIX_LENGTH) {
            return Mono.error(new CommandException(String.format("Prefix cannot contain more than %s characters.",
                    MAX_PREFIX_LENGTH)));
        }

        if (args.get(1).contains(" ")) {
            return Mono.error(new CommandException("Prefix cannot contain spaces."));
        }

        DatabaseManager.getGuilds().getDBGuild(context.getGuildId()).setSetting(this.getSetting(), args.get(1));
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Prefix set to `%s`", args.get(1)), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <prefix>`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", "**prefix** - Max length: 5, must not contain spaces", false)
                        .addField("Example", String.format("`%s%s !`", context.getPrefix(), this.getCommandName()), false));
    }

}
