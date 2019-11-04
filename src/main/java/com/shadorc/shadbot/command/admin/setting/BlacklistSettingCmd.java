package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.guild.entity.DBGuild;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BlacklistSettingCmd extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public BlacklistSettingCmd() {
        super(Setting.BLACKLIST, "Manage blacklisted commands.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final List<String> commands = StringUtils.split(args.get(2).toLowerCase());

        final List<String> unknownCmds = commands.stream()
                .filter(cmd -> CommandManager.getInstance().getCommand(cmd) == null)
                .collect(Collectors.toList());

        if (!unknownCmds.isEmpty()) {
            return Mono.error(new CommandException(String.format("Command %s doesn't exist.",
                    FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", "))));
        }

        for (final String settingCmdName : CommandManager.getInstance().getCommand("setting").getNames()) {
            if (commands.contains(settingCmdName)) {
                return Mono.error(new CommandException(String.format("You cannot blacklist the command `%s%s`.",
                        context.getPrefix(), settingCmdName)));
            }
        }

        final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(context.getGuildId());
        final List<String> blacklist = dbGuild.getSettings().getBlacklistedCmd();

        final String actionVerbose;
        if (action == Action.ADD) {
            blacklist.addAll(commands);
            actionVerbose = "added";
        } else {
            blacklist.removeAll(commands);
            actionVerbose = "removed";
        }

        dbGuild.setSetting(this.getSetting(), blacklist);
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command(s) %s %s to the blacklist.",
                        FormatUtils.format(commands, cmd -> String.format("`%s`", cmd), ", "), actionVerbose),
                        channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <command(s)>`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s",
                                FormatUtils.format(Action.class, "/")), false)
                        .addField("Example", String.format("`%s%s add rule34 russian_roulette`", context.getPrefix(), this.getCommandName()), false));
    }

}
