package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.BaseSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

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

		final Action action = Utils.getEnum(Action.class, args.get(1));
		if(action == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid action. %s", 
					args.get(1), FormatUtils.options(Action.class))));
		}

		final List<String> commands = StringUtils.split(args.get(2).toLowerCase());

		final List<String> unknownCmds = commands.stream().filter(cmd -> CommandInitializer.getCommand(cmd) == null).collect(Collectors.toList());
		if(!unknownCmds.isEmpty()) {
			return Mono.error(new CommandException(String.format("Command %s doesn't exist.",
					FormatUtils.format(unknownCmds, cmd -> String.format("`%s`", cmd), ", "))));
		}

		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
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
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Command(s) `%s` %s to the blacklist.",
						FormatUtils.format(commands, cmd -> String.format("`%s`", cmd), ", "), actionVerbose),
						channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <command(s)>`", context.getPrefix(), this.getCommandName()), false)
						.addField("Argument", String.format("**action** - %s",
								FormatUtils.format(Action.class, "/")), false)
						.addField("Example", String.format("`%s%s add rule34 russian_roulette`", context.getPrefix(), this.getCommandName()), false));
	}

}
