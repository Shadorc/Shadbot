package me.shadorc.shadbot.command.hidden;

import java.util.stream.Collectors;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.HIDDEN, names = { "help" })
public class HelpCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		if(context.hasArg()) {
			AbstractCommand cmd = CommandManager.getCommand(context.getArg());
			if(cmd == null) {
				return;
			}

			BotUtils.sendMessage(cmd.getHelp(context), context.getChannel());
			return;
		}

		EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Shadbot Help")
				.appendDescription(String.format("Get more information by using `%s%s <command>`.", context.getPrefix(), this.getName()))
				.withFooterText(String.format("Any issues, questions or suggestions ? Join %s", Config.SUPPORT_SERVER));

		for(CommandCategory category : CommandCategory.values()) {
			if(category.equals(CommandCategory.HIDDEN)) {
				continue;
			}

			StringBuilder contentBuilder = new StringBuilder();

			for(AbstractCommand cmd : CommandManager.getCommands().values().stream().distinct().collect(Collectors.toList())) {
				if(!cmd.getCategory().equals(category) || context.getPermission().getHierarchy() < cmd.getPermission().getHierarchy()) {
					continue;
				}

				// TODO: && BotUtils.isCommandAllowed(context.getGuild(), cmd)

				contentBuilder.append("`" + context.getPrefix() + cmd.getName() + "` ");
			}

			builder.appendField(String.format("%s Commands", category.toString()), contentBuilder.toString(), false);
		}

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show the list of available commands.")
				.build();
	}

}
