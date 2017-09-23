package me.shadorc.discordbot.command.hidden;

import java.time.temporal.ChronoUnit;
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

	private final RateLimiter rateLimiter;

	public HelpCmd() {
		super(CommandCategory.HIDDEN, Role.USER, "help", "aide");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(context.hasArg() && CommandManager.getCommand(context.getArg()) != null) {
			CommandManager.getCommand(context.getArg()).showHelp(context);
			return;
		}

		String prefix = context.getPrefix();
		Role authorRole = context.getAuthorRole();

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Shadbot Help")
				.appendDescription("Get more information by using " + prefix + "help <command>.")
				.withFooterIcon("http://www.urbanleagueneb.org/wp-content/uploads/2016/10/E-mail-Icon.png")
				.withFooterText("You can send me a suggestion, a bug report, anything by using: " + prefix + "report <message>");

		Arrays.stream(CommandCategory.values())
				.filter(cmdCat -> !cmdCat.equals(CommandCategory.HIDDEN))
				.forEach(category -> builder.appendField(category.toString() + " Commands:",
						CommandManager.getCommands().values().stream()
								.filter(cmd -> cmd.getCategory().equals(category) && authorRole.getHierarchy() >= cmd.getRole().getHierarchy())
								.distinct()
								.map(cmd -> "`" + prefix + cmd.getNames()[0] + "`")
								.collect(Collectors.joining(" ")), false));

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show help for commands.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
