package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class AutoMessageSetting extends BaseSetting {

    private enum Action {
        ENABLE, DISABLE;
    }

    private enum Type {
        CHANNEL, JOIN_MESSAGE, LEAVE_MESSAGE;
    }

    public AutoMessageSetting() {
        super(Setting.AUTO_MESSAGE, "Manage auto messages on user join/leave.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3, 4);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = Utils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> {
                    switch (type) {
                        case CHANNEL:
                            return AutoMessageSetting.channel(context, dbGuild, action);
                        case JOIN_MESSAGE:
                            return this.updateMessage(context, dbGuild, Setting.JOIN_MESSAGE, action, args);
                        case LEAVE_MESSAGE:
                            return this.updateMessage(context, dbGuild, Setting.LEAVE_MESSAGE, action, args);
                        default:
                            return Mono.empty();
                    }
                })
                .then();
    }

    private static Mono<Message> channel(Context context, DBGuild dbGuild, Action action) {
        if (action == Action.DISABLE) {
            return dbGuild.removeSetting(Setting.MESSAGE_CHANNEL_ID)
                    .then(context.getChannel())
                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " Auto-messages disabled. " +
                            "I will no longer send auto-messages until a new channel is defined.", channel));
        }

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, context.getContent()))
                .collectList()
                .flatMap(mentionedChannels -> {
                    if (mentionedChannels.size() != 1) {
                        return Mono.error(new MissingArgumentException());
                    }

                    final Channel channel = mentionedChannels.get(0);
                    return dbGuild.setSetting(Setting.MESSAGE_CHANNEL_ID, channel.getId().asLong())
                            .thenReturn(String.format(Emoji.CHECK_MARK + " %s is now the default channel for " +
                                    "join/leave messages.", channel.getMention()));
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)));
    }

    private Mono<Message> updateMessage(Context context, DBGuild dbGuild, Setting setting, Action action, List<String> args) {
        return Mono.just(new StringBuilder())
                .flatMap(strBuilder -> {
                    if (action == Action.ENABLE) {
                        if (args.size() < 4) {
                            return Mono.error(new MissingArgumentException());
                        }

                        final String message = args.get(3);
                        return dbGuild.setSetting(setting, message)
                                .then(Mono.fromCallable(() -> {
                                    if (dbGuild.getSettings().getMessageChannelId().isEmpty()) {
                                        strBuilder.append(String.format(Emoji.WARNING + " You need to specify a channel "
                                                        + "in which to send the auto-messages. Use `%s%s %s %s <#channel>`%n",
                                                context.getPrefix(), this.getCommandName(), Action.ENABLE.toString().toLowerCase(),
                                                Type.CHANNEL.toString().toLowerCase()));
                                    }
                                    return strBuilder.append(String.format(Emoji.CHECK_MARK + " %s set to `%s`",
                                            StringUtils.capitalizeEnum(setting), message));
                                }));

                    } else {
                        return dbGuild.removeSetting(setting)
                                .thenReturn(strBuilder.append(
                                        String.format(Emoji.CHECK_MARK + " %s disabled.", StringUtils.capitalizeEnum(setting))));
                    }
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <type> [<value>]`",
                        context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s"
                                        + "%n**type** - %s"
                                        + "%n**value** - a message for *%s* and *%s* or a #channel for *%s*",
                                FormatUtils.format(Action.class, "/"),
                                FormatUtils.format(Type.class, "/"),
                                Type.JOIN_MESSAGE.toString().toLowerCase(),
                                Type.LEAVE_MESSAGE.toString().toLowerCase(),
                                Type.CHANNEL.toString().toLowerCase()), false)
                        .addField("Info", "You don't need to specify *value* to disable a type.", false)
                        .addField("Formatting", "**{mention}** - the mention of the user who joined/left"
                                + "\n**{username}** - the username of the user who joined/left"
                                + "\n**{userId}** - the id of the user who joined/left", false)
                        .addField("Example", String.format("`%s%s enable join_message Hello {mention} (:`"
                                        + "%n`%s%s disable leave_message`",
                                context.getPrefix(), this.getCommandName(), context.getPrefix(), this.getCommandName()), false));
    }
}
