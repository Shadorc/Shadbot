package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class NSFWSetting extends BaseCmd {

    private enum Action {
        TOGGLE, ENABLE, DISABLE
    }

    public NSFWSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "nsfw", "Manage current channel's NSFW state.");

        this.addOption(option -> option.name("action")
                .description("Change the NSFW state of the server")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();

        return context.getChannel()
                .cast(TextChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_CHANNELS)
                        .then(Mono.fromSupplier(() -> switch (action) {
                            case TOGGLE -> !channel.isNsfw();
                            case ENABLE -> true;
                            case DISABLE -> false;
                        }))
                        .flatMap(nsfw -> channel.edit(spec -> spec.setNsfw(nsfw))))
                .map(channel -> channel.isNsfw()
                        ? context.localize("setting.nsfw.nsfw").formatted(channel.getMention())
                        : context.localize("setting.nsfw.sfw").formatted(channel.getMention()))
                .flatMap(message -> context.reply(Emoji.CHECK_MARK, message));
    }

}
