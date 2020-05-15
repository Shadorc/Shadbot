package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BlacklistSettingCmd extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    private enum Type {
        COMMAND, CATEGORY;
    }

    public BlacklistSettingCmd() {
        super(List.of("blacklist"),
                Setting.BLACKLIST, "Manage blacklisted command(s).");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(4);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final Type type = Utils.parseEnum(Type.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid type. %s",
                        args.get(2), FormatUtils.options(Type.class))));

        final String names = args.get(3);

        switch (type) {
            case COMMAND:
                return this.blacklistCommands(context, action, StringUtils.split(names.toLowerCase()));
            case CATEGORY:
                return this.blacklistCategories(context, action, StringUtils.split(names.toUpperCase()));
            default:
                return Mono.error(new IllegalStateException(String.format("Unknown type: %s", type)));
        }
    }

    private Mono<Void> blacklistCategories(Context context, Action action, List<String> categoryNames) {
        final Set<String> unknownCategories = categoryNames.stream()
                .filter(category -> Utils.parseEnum(CommandCategory.class, category) == null)
                .collect(Collectors.toSet());

        if (!unknownCategories.isEmpty()) {
            return Mono.error(new CommandException(String.format("Category %s doesn't exist.",
                    FormatUtils.format(unknownCategories, category -> String.format("`%s`", category), ", "))));
        }

        final Set<CommandCategory> categories = categoryNames.stream()
                .map(category -> Utils.parseEnum(CommandCategory.class, category))
                .collect(Collectors.toSet());

        // Do not allow to blacklist admin category
        if (categories.contains(CommandCategory.ADMIN)) {
            return Mono.error(new CommandException("You cannot blacklist the whole admin category."));
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> {
                    final Set<String> blacklist = dbGuild.getSettings().getBlacklistedCmd();

                    final Set<String> cmdNames = new HashSet<>();
                    for (final CommandCategory category : categories) {
                        for (final BaseCmd cmd : CommandManager.getInstance().getCommands().values()) {
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

                    return dbGuild.setSetting(this.getSetting(), blacklist)
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
                .filter(cmd -> CommandManager.getInstance().getCommand(cmd) == null)
                .collect(Collectors.toUnmodifiableSet());

        if (!unknownCmds.isEmpty()) {
            return Mono.error(new CommandException(String.format("Command %s doesn't exist.",
                    FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", "))));
        }

        // Do not allow to blacklist setting command
        for (final String settingCmdName : CommandManager.getInstance().getCommand("setting").getNames()) {
            if (cmdNames.contains(settingCmdName)) {
                return Mono.error(new CommandException(String.format("You cannot blacklist the command `%s%s`.",
                        context.getPrefix(), settingCmdName)));
            }
        }

        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMap(dbGuild -> {
                    final Set<String> blacklist = dbGuild.getSettings().getBlacklistedCmd();

                    final String actionVerbose;
                    if (action == Action.ADD) {
                        blacklist.addAll(cmdNames);
                        actionVerbose = "added";
                    } else {
                        blacklist.removeAll(cmdNames);
                        actionVerbose = "removed";
                    }

                    return dbGuild.setSetting(this.getSetting(), blacklist)
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
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <type> <name(s)>`",
                        context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument",
                                String.format("**action** - %s", FormatUtils.format(Action.class, "/"))
                                        + String.format("%n**type** - %s", FormatUtils.format(Type.class, "/")), false)
                        .addField("Example", String.format("`%s%s add command rule34 russian_roulette`" +
                                        "%n`%s%s add category NSFW`",
                                context.getPrefix(), this.getCommandName(),
                                context.getPrefix(), this.getCommandName()), false));
    }

}
