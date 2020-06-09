package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class PrefixSetting extends BaseSetting {

    private static final int MAX_PREFIX_LENGTH = 5;

    public PrefixSetting() {
        super(List.of("prefix"),
                Setting.PREFIX, "Manage Shadbot's prefix.");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Mono.just(settings.getPrefix())
                .filter(prefix -> !prefix.equals(Config.DEFAULT_PREFIX))
                .map(prefix -> ImmutableEmbedFieldData.builder()
                        .name("Prefix")
                        .value(prefix)
                        .build());
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

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> dbGuild.updateSetting(this.getSetting(), args.get(1)))
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Prefix set to `%s`",
                        args.get(1)), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("prefix", "new prefix", false)
                .setExample(String.format("`%s%s !`", context.getPrefix(), this.getCommandName()))
                .addField("Restrictions", "The prefix cannot contain spaces nor more than 6 characters.", false)
                .build();
    }
}
