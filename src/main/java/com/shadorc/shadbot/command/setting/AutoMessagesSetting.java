package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.database.guilds.bean.setting.AutoMessageBean;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class AutoMessagesSetting extends BaseCmd {

    private enum Action {
        ENABLE, DISABLE
    }

    private enum Type {
        JOIN_MESSAGE, LEAVE_MESSAGE
    }

    public AutoMessagesSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "auto_messages", "Manage auto-message(s) on user join/leave");

        this.addOption("action", "Whether to enable or disable automatic messages", true,
                ApplicationCommandOptionType.STRING, DiscordUtil.toOptions(Action.class));
        this.addOption("type", "The type of automatic message to configure", true,
                ApplicationCommandOptionType.STRING, DiscordUtil.toOptions(Type.class));
        this.addOption("message", "The message to automatically send", false, ApplicationCommandOptionType.STRING);
        this.addOption("channel", "The channel in which send the automatic message", false, ApplicationCommandOptionType.CHANNEL);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Type type = context.getOptionAsEnum(Type.class, "type").orElseThrow();

        return this.updateMessage(context, action, type);
    }

    private Mono<Message> updateMessage(Context context, Action action, Type type) {
        final String typeStr = type == Type.JOIN_MESSAGE ? "join" : "leave";
        final Setting setting = type == Type.JOIN_MESSAGE ? Setting.AUTO_JOIN_MESSAGE : Setting.AUTO_LEAVE_MESSAGE;
        switch (action) {
            case ENABLE:
                final Optional<String> messageOpt = context.getOptionAsString("message");
                if (messageOpt.isEmpty()) {
                    return Mono.error(new CommandException(context.localize("automessage.missing.message")));
                }
                final String message = messageOpt.orElseThrow();
                return context.getOptionAsChannel("channel")
                        .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.missing.channel"))))
                        .ofType(TextChannel.class)
                        .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.exception.channel.type"))))
                        .flatMap(channel -> context.getDbGuild()
                                .updateSetting(setting, new AutoMessageBean(message, channel.getId().asString()))
                                .then(context.reply(Emoji.CHECK_MARK, context.localize("automessage." + typeStr + ".enabled")
                                        .formatted(message, channel.getMention()))));

            case DISABLE:
                return context.getDbGuild().removeSetting(setting)
                        .then(context.reply(Emoji.CHECK_MARK, context.localize("automessage." + typeStr + ".disabled")));

            default:
                return Mono.error(new IllegalStateException());
        }
    }

}
