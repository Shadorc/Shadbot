package me.shadorc.discordbot.command.hidden;

import java.util.Arrays;
import java.util.stream.Collectors;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends AbstractCommand {

	public HelpCmd() {
		super(CommandCategory.HIDDEN, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "help");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.hasArg() && CommandManager.getCommand(context.getArg()) != null) {
			CommandManager.getCommand(context.getArg()).showHelp(context);
			return;
		}

		String prefix = context.getPrefix();
		Role authorRole = context.getAuthorRole();

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Shadbot Help")
				.appendDescription("Get more information by using `" + prefix + "help <command>`.")
				.withFooterText("Any issues, questions or suggestions ? Join https://discord.gg/CKnV4ff");

		Arrays.stream(CommandCategory.values())
				.filter(cmdCat -> !cmdCat.equals(CommandCategory.HIDDEN))
				.forEach(category -> builder.appendField(category.toString() + " Commands:",
						CommandManager.getCommands().values().stream()
								.filter(cmd -> cmd.getCategory().equals(category)
										&& authorRole.getHierarchy() >= cmd.getRole().getHierarchy()
										&& BotUtils.isCommandAllowed(context.getGuild(), cmd))
								.distinct()
								.map(cmd -> "`" + prefix + cmd.getFirstName() + "`")
								.collect(Collectors.joining(" ")), false));

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show help for commands.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
