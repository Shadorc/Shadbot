/*
package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class NSFWSetting extends BaseSetting {

    private enum Action {
        TOGGLE, ENABLE, DISABLE
    }

    public NSFWSetting() {
        super(List.of("nsfw"),
                Setting.NSFW, "Manage current channel's NSFW state.");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getChannel()
                .cast(TextChannel.class)
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.MANAGE_CHANNELS)
                        .then(Mono.fromSupplier(() -> switch (action) {
                            case TOGGLE -> !channel.isNsfw();
                            case ENABLE -> true;
                            default -> false;
                        }))
                        .flatMap(nsfw -> channel.edit(spec -> spec.setNsfw(nsfw))))
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (**%s**) %s is now **%sSFW**.",
                        context.getUsername(), channel.getMention(), channel.isNsfw() ? "N" : ""), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .setExample(String.format("`%s%s toggle`", context.getPrefix(), this.getCommandName()))
                .build();
    }

}
*/
