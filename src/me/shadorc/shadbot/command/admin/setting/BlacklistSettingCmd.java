package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Mono;

@Setting(description = "Manage blacklisted commands.", setting = SettingEnum.BLACKLIST)
public class BlacklistSettingCmd extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(3);
		args.remove(0);

		final Action action = Utils.getEnum(Action.class, args.get(0));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(0), FormatUtils.options(Action.class)));
		}

		final List<String> commands = StringUtils.split(args.get(1).toLowerCase());

		final List<String> unknownCmds = commands.stream().filter(cmd -> CommandManager.getCommand(cmd) == null).collect(Collectors.toList());
		if(!unknownCmds.isEmpty()) {
			throw new CommandException(String.format("Command %s doesn't exist.",
					FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", ")));
		}

		final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
		final List<String> blacklist = dbGuild.getBlacklistedCmd();

		String actionVerbose;
		if(Action.ADD.equals(action)) {
			blacklist.addAll(commands);
			actionVerbose = "added";
		} else {
			blacklist.removeAll(commands);
			actionVerbose = "removed";
		}

		dbGuild.setSetting(this.getSetting(), blacklist);
		return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command(s) `%s` %s to the blacklist.",
				FormatUtils.format(commands, cmd -> String.format("`%s`", cmd), ", "), actionVerbose),
				context.getChannel()).then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <command(s)>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s add rule34 russian_roulette`", context.getPrefix(), this.getCommandName()), false);
	}

}
