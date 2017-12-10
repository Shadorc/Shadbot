package me.shadorc.discordbot.command.french;

import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.TwitterUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;
import twitter4j.TwitterException;

public class HolidaysCmd extends AbstractCommand {

	private static final List<String> ZONES = Arrays.asList("A", "B", "C");

	public HolidaysCmd() {
		super(CommandCategory.FRENCH, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "vacs", "vacances");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String zone = context.getArg().toUpperCase();
		if(!ZONES.contains(zone)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid zone. Options: A, B, C", context.getChannel());
			return;
		}

		try {
			TwitterUtils.connection();
			String holidays = TwitterUtils.getInstance().getUserTimeline("Vacances_Zone" + zone).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException err) {
			ExceptionUtils.manageException("getting holidays information", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show the number of remaining days before the next school holidays for the indicated zone.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <A|B|C>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
