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
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BlacklistSettingCmd extends BaseSetting {

    private enum Action {
        ADD, REMOVE
    }

    private enum Type {
        COMMAND, CATEGORY
    }

    public BlacklistSettingCmd() {
        super(List.of("blacklist"),
                Setting.BLACKLIST, "Manage blacklisted command(s).");
    }

    @Override
    public Mono<ImmutableEmbedFieldData> show(Context context, Settings settings) {
        return Mono.just(settings.getBlacklistedCmds())
                .filter(blacklistedCmds -> !blacklistedCmds.isEmpty())
                .map(blacklistedCmds -> ImmutableEmbedFieldData.builder()
                        .name("Blacklisted commands")
                        .value(String.join(", ", blacklistedCmds))
                        .inline(false)
                        .build());
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(4);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = EnumUtils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        final String names = args.get(3);

        return switch (type) {
            case COMMAND -> this.blacklistCommands(context, action, StringUtils.split(names.toLowerCase()));
            case CATEGORY -> this.blacklistCategories(context, action, StringUtils.split(names.toUpperCase()));
        };
    }

    private Mono<Void> blacklistCategories(Context context, Action action, List<String> categoryNames) {
        final Set<String> unknownCategories = categoryNames.stream()
                .filter(category -> EnumUtils.parseEnum(CommandCategory.class, category) == null)
                .collect(Collectors.toSet());

        if (!unknownCategories.isEmpty()) {
            return Mono.error(new CommandException(String.format("Category %s doesn't exist.",
                    FormatUtils.format(unknownCategories, category -> String.format("`%s`", category), ", "))));
        }

        final Set<CommandCategory> categories = categoryNames.stream()
                .map(category -> EnumUtils.parseEnum(CommandCategory.class, category))
                .collect(Collectors.toSet());

        // Do not allow to blacklist admin category
        if (categories.contains(CommandCategory.ADMIN)) {
            return Mono.error(new CommandException("You cannot blacklist the whole admin category."));
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> {
                    final Set<String> blacklist = dbGuild.getSettings().getBlacklistedCmds();

                    final Set<String> cmdNames = new HashSet<>();
                    for (final CommandCategory category : categories) {
                        for (final BaseCmd cmd : CommandManager.getCommands().values()) {
                            if (cmd.getCategory() == category) {
                                cmdNames.add(cmd.getName());
                            }
                        }
                    }

                    final String actionVerbose;
                    if (action == Action.ADD) {
                        blacklist.addAll(cmdNames);
                        actionVerbose = "added";
                    } else {
                        blacklist.removeAll(cmdNames);
                        actionVerbose = "removed";
                    }

                    return dbGuild.updateSetting(this.getSetting(), blacklist)
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.CHECK_MARK + " Category %s %s to the blacklist.",
                                            FormatUtils.format(categoryNames, name -> String.format("`%s`", name), ", "),
                                            actionVerbose),
                                    channel));
                })
                .then();
    }

    private Mono<Void> blacklistCommands(Context context, Action action, List<String> cmdNames) {
        final Set<String> unknownCmds = cmdNames.stream()
                .filter(cmd -> CommandManager.getCommand(cmd) == null)
                .collect(Collectors.toUnmodifiableSet());

        if (!unknownCmds.isEmpty()) {
            return Mono.error(new CommandException(String.format("Command %s doesn't exist.",
                    FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", "))));
        }

        // Do not allow to blacklist setting command
        for (final String settingCmdName : CommandManager.getCommand("setting").getNames()) {
            if (cmdNames.contains(settingCmdName)) {
                return Mono.error(new CommandException(String.format("You cannot blacklist the command `%s%s`.",
                        context.getPrefix(), settingCmdName)));
            }
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> {
                    final Set<String> blacklist = dbGuild.getSettings().getBlacklistedCmds();

                    final String actionVerbose;
                    if (action == Action.ADD) {
                        blacklist.addAll(cmdNames);
                        actionVerbose = "added";
                    } else {
                        blacklist.removeAll(cmdNames);
                        actionVerbose = "removed";
                    }

                    return dbGuild.updateSetting(this.getSetting(), blacklist)
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.CHECK_MARK + " Command(s) %s %s to the blacklist.",
                                            FormatUtils.format(cmdNames, name -> String.format("`%s`", name), ", "),
                                            actionVerbose),
                                    channel));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return SettingHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.format(Action.class, "/"), false)
                .addArg("type", FormatUtils.format(Type.class, "/"), false)
                .addArg("name(s)", "command/category name", false)
                .setExample(String.format("`%s%s add command rule34 russian_roulette`" +
                                "%n`%s%s add category NSFW`",
                        context.getPrefix(), this.getCommandName(),
                        context.getPrefix(), this.getCommandName()))
                .build();
    }

}
*/
