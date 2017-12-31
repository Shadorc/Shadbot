package me.shadorc.shadbot.command.french;

import java.util.Arrays;
import java.util.List;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TwitterUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import twitter4j.TwitterException;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private static final List<String> ZONES = Arrays.asList("A", "B", "C");

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String zone = context.getArg().toUpperCase();
		if(!ZONES.contains(zone)) {
			throw new IllegalArgumentException("Invalid zone. Options: " + FormatUtils.formatList(ZONES, opt -> opt.toString(), ", "));
		}

		try {
			String holidays = TwitterUtils.getLastTweet("Vacances_Zone" + zone).replace("#", "");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException err) {
			ExceptionUtils.handle("getting holidays information", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show the number of remaining days before the next school holidays for the indicated zone.")
				.addArg(ZONES, false)
				.build();
	}

}
