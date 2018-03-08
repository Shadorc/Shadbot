package me.shadorc.shadbot.command.hidden;

import java.util.stream.Collectors;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "help" })
public class HelpCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(context.hasArg()) {
			AbstractCommand cmd = CommandManager.getCommand(context.getArg());
			if(cmd == null) {
				return;
			}

			BotUtils.sendMessage(cmd.getHelp(context.getPrefix()), context.getChannel());
			CommandStatsManager.log(CommandEnum.COMMAND_HELPED, cmd);
			return;
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Shadbot Help")
				.appendDescription(String.format("Any issues, questions or suggestions ? Join the [support server.](%s)"
						+ "%nGet more information by using `%s%s <command>`.",
						Config.SUPPORT_SERVER, context.getPrefix(), this.getName()));

		for(CommandCategory category : CommandCategory.values()) {
			if(category.equals(CommandCategory.HIDDEN)) {
				continue;
			}

			String commands = CommandManager.getCommands().values().stream()
					.distinct()
					.filter(cmd -> cmd.getCategory().equals(category)
							&& !cmd.getPermission().isSuperior(context.getAuthorPermission())
							&& (context.getGuild() == null || BotUtils.isCommandAllowed(context.getGuild(), cmd)))
					.map(cmd -> String.format("`%s%s`", context.getPrefix(), cmd.getName()))
					.collect(Collectors.joining(" "));

			embed.appendField(String.format("%s Commands", category.toString()), commands, false);
		}

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show the list of available commands.")
				.build();
	}

}
