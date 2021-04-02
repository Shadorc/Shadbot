/*
package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.SettingHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RestrictedChannelsSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE
    }

    private enum Type {
        COMMAND, CATEGORY
    }

    public RestrictedChannelsSetting() {
        super(List.of("restricted_channels", "restricted_channel"),
                Setting.RESTRICTED_CHANNELS, "Restrict commands to specific channels.");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Flux.fromIterable(settings.getRestrictedChannels().entrySet())
                .flatMap(entry -> Mono.zip(
                        context.getClient().getChannelById(entry.getKey()).cast(TextChannel.class).map(TextChannel::getMention),
                        Mono.just(FormatUtils.format(entry.getValue(), BaseCmd::getName, ", "))))
                .map(TupleUtils.function((channelMention, cmds) -> String.format("%s: %s", channelMention, cmds)))
                .reduce("", (value, text) -> value + "\n" + text)
                .filter(value -> !value.isBlank())
                .map(value -> ImmutableEmbedFieldData.builder()
                        .name("Restricted channels")
                        .value(value)
                        .build());
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(5);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = EnumUtils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        final String name = args.get(3);
        final Set<BaseCmd> commands = new HashSet<>();
        switch (type) {
            case COMMAND -> {
                final BaseCmd command = CommandManager.getCommand(name);
                if (command == null) {
                    return Mono.error(new CommandException(String.format("`%s` is not a valid command.", name)));
                }
                commands.add(command);
            }
            case CATEGORY -> {
                final CommandCategory category = EnumUtils.parseEnum(CommandCategory.class, name,
                        new CommandException(String.format("`%s` is not a valid category. %s",
                                name, FormatUtils.options(CommandCategory.class))));
                commands.addAll(CommandManager.getCommands().values().stream()
                        .filter(cmd -> cmd.getCategory() == category)
                        .collect(Collectors.toSet()));
            }
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
                .flatMap(TupleUtils.function((mentionedChannel, dbGuild) -> {
                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedCategories = dbGuild.getSettings()
                            .getRestrictedChannels();
                    switch (action) {
                        case ADD -> {
                            restrictedCategories.computeIfAbsent(mentionedChannel.getId(), __ -> new HashSet<>())
                                    .addAll(commands);
                            strBuilder.append(
                                    String.format("Command(s) %s can now only be used in channel **#%s**.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " "),
                                            mentionedChannel.getName()));
                        }
                        case REMOVE -> {
                            if (restrictedCategories.containsKey(mentionedChannel.getId())) {
                                restrictedCategories.get(mentionedChannel.getId()).removeAll(commands);
                            }
                            strBuilder.append(
                                    String.format("Command(s) %s can now be used everywhere.",
                                            FormatUtils.format(commands, cmd -> String.format("`%s`", cmd.getName()), " ")));
                        }
                    }

                    final Map<String, Set<String>> setting = restrictedCategories
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(BaseCmd::getName)
                                            .collect(Collectors.toSet())));

                    return dbGuild.updateSetting(Setting.RESTRICTED_CHANNELS, setting)
                            .and(context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " " + strBuilder, channel)));
                }))
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
*/
