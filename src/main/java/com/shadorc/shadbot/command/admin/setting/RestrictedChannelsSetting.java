package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RestrictedChannelsSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    private enum Type {
        COMMAND, CATEGORY;
    }

    public RestrictedChannelsSetting() {
        super(List.of("restricted_channels", "restricted_channel"),
                Setting.RESTRICTED_CHANNELS, "Restrict commands to specific channels.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(5);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = Utils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        final String name = args.get(3);
        final Set<BaseCmd> commands = new HashSet<>();
        switch (type) {
            case COMMAND:
                final BaseCmd command = CommandManager.getInstance().getCommand(name);
                if (command == null) {
                    return Mono.error(new CommandException(String.format("`%s` is not a valid command.", name)));
                }
                commands.add(command);
                break;
            case CATEGORY:
                final CommandCategory category = Utils.parseEnum(CommandCategory.class, name,
                        new CommandException(String.format("`%s` is not a valid category. %s",
                                name, FormatUtils.options(CommandCategory.class))));
                commands.addAll(CommandManager.getInstance().getCommands().values().stream()
                        .filter(cmd -> cmd.getCategory() == category)
                        .collect(Collectors.toSet()));
                break;
        }

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, args.get(4)))
                .collectList()
                .flatMap(mentionedChannels -> {
                    if (mentionedChannels.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Channel `%s` not found.", args.get(4))));
                    }

                    return Mono.zip(Mono.just(mentionedChannels.get(0)),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(tuple -> {
                    final GuildChannel mentionedChannel = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedCategories = dbGuild.getSettings()
                            .getRestrictedChannels();
                    switch (action) {
                        case ADD:
                            restrictedCategories.computeIfAbsent(mentionedChannel.getId(), ignored -> new HashSet<>())
                                    .addAll(commands);
                            strBuilder.append(
                                    String.format("Command(s) %s can now only be used in channel **#%s**.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " "),
                                            mentionedChannel.getName()));
                            break;
                        case REMOVE:
                            if (restrictedCategories.containsKey(mentionedChannel.getId())) {
                                restrictedCategories.get(mentionedChannel.getId()).removeAll(commands);
                            }
                            strBuilder.append(
                                    String.format("Command(s) %s can now be used everywhere.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " ")));
                            break;
                    }

                    final Map<String, Set<String>> setting = restrictedCategories
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(BaseCmd::getName)
                                            .collect(Collectors.toSet())));

                    return dbGuild.setSetting(Setting.RESTRICTED_CHANNELS, setting)
                            .and(context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " " + strBuilder, channel)));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("type", FormatUtils.format(Type.class, "/"), false)
                .addArg("name", "command/category name", false)
                .addArg("channel", String.format("the channel to %s", FormatUtils.format(Action.class, "/")), false)
                .setExample(String.format("`%s%s add category music #music`" +
                                "%n`%s%s add command rule34 #nsfw`",
                        context.getPrefix(), this.getCommandName(), context.getPrefix(), this.getCommandName()))
                .build();
    }
}
