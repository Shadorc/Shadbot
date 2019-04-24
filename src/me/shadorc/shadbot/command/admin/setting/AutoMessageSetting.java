package me.shadorc.shadbot.command.admin.setting;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.BaseSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
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

        switch (type) {
            case CHANNEL:
                return this.channel(context, action).then();
            case JOIN_MESSAGE:
                return this.updateMessage(context, Setting.JOIN_MESSAGE, action, args).then();
            case LEAVE_MESSAGE:
                return this.updateMessage(context, Setting.LEAVE_MESSAGE, action, args).then();
            default:
                return Mono.empty();
        }
    }

    private Mono<Message> channel(Context context, Action action) {
        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, context.getContent()))
                .flatMap(channelId -> context.getClient().getChannelById(channelId))
                .collectList()
                .map(mentionedChannels -> {
                    if (mentionedChannels.size() != 1) {
                        throw new MissingArgumentException();
                    }

                    final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
                    final Channel channel = mentionedChannels.get(0);
                    if (Action.ENABLE.equals(action)) {
                        dbGuild.setSetting(Setting.MESSAGE_CHANNEL_ID, channel.getId().asLong());
                        return String.format(Emoji.CHECK_MARK + " %s is now the default channel for join/leave messages.",
                                channel.getMention());
                    } else {
                        dbGuild.removeSetting(Setting.MESSAGE_CHANNEL_ID);
                        return String.format(Emoji.CHECK_MARK + " Auto-messages disabled. I will no longer send auto-messages "
                                + "until a new channel is defined.", channel.getMention());
                    }
                })
                .flatMap(message -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(message, channel)));
    }

    private Mono<Message> updateMessage(Context context, Setting setting, Action action, List<String> args) {
        final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
        final StringBuilder strBuilder = new StringBuilder();
        if (Action.ENABLE.equals(action)) {
            if (args.size() < 4) {
                return Mono.error(new MissingArgumentException());
            }
            final String message = args.get(3);
            dbGuild.setSetting(setting, message);

            if (dbGuild.getMessageChannelId().isEmpty()) {
                strBuilder.append(String.format(Emoji.WARNING + " You need to specify a channel "
                                + "in which send the auto-messages. Use `%s%s %s %s <#channel>`%n",
                        context.getPrefix(), this.getCommandName(), StringUtils.toLowerCase(Action.ENABLE), StringUtils.toLowerCase(Type.CHANNEL)));
            }
            strBuilder.append(String.format(Emoji.CHECK_MARK + " %s set to `%s`", StringUtils.capitalizeEnum(setting), message));

        } else {
            dbGuild.removeSetting(setting);
            strBuilder.append(String.format(Emoji.CHECK_MARK + " %s disabled.", StringUtils.capitalizeEnum(setting)));
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return EmbedUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <type> [<value>]`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s"
                                        + "%n**type** - %s"
                                        + "%n**value** - a message for *%s* and *%s* or a #channel for *%s*",
                                FormatUtils.format(Action.class, "/"),
                                FormatUtils.format(Type.class, "/"),
                                StringUtils.toLowerCase(Type.JOIN_MESSAGE),
                                StringUtils.toLowerCase(Type.LEAVE_MESSAGE),
                                StringUtils.toLowerCase(Type.CHANNEL)), false)
                        .addField("Info", "You don't need to specify *value* to disable a type.", false)
                        .addField("Formatting", "**{mention}** - the mention of the user who joined/left"
                                + "\n**{username}** - the username of the user who joined/left"
                                + "\n**{userId}** - the id of the user who joined/left", false)
                        .addField("Example", String.format("`%s%s enable join_message Hello {mention} (:`"
                                        + "%n`%s%s disable leave_message`",
                                context.getPrefix(), this.getCommandName(), context.getPrefix(), this.getCommandName()), false));
    }
}
