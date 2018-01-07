package me.shadorc.shadbot.command.french;

import java.util.Arrays;
import java.util.List;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TwitterUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import twitter4j.TwitterException;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private static final List<String> ZONES = Arrays.asList("A", "B", "C");

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String zone = context.getArg().toUpperCase();
		if(!ZONES.contains(zone)) {
			throw new IllegalCmdArgumentException("Invalid zone. Options: " + FormatUtils.format(ZONES, Object::toString, ", "));
		}

		try {
			String holidays = StringUtils.remove(TwitterUtils.getLastTweet("Vacances_Zone" + zone), "#");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException err) {
			ExceptionUtils.handle("getting holidays information", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show the number of remaining days before the next school holidays for the indicated zone.")
				.addArg(ZONES, false)
				.build();
	}

}
