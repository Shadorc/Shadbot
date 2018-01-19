package me.shadorc.shadbot.command.french;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TwitterUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import twitter4j.TwitterException;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private enum Zone {
		A, B, C;
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Zone zone = Utils.getValueOrNull(Zone.class, context.getArg());
		if(zone == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid zone. %s",
					context.getArg(), FormatUtils.formatOptions(Zone.class)));
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading holiday information...", context.getChannel());
		loadingMsg.send();

		try {
			String holidays = StringUtils.remove(TwitterUtils.getLastTweet("Vacances_Zone" + zone), "#");
			loadingMsg.edit(Emoji.BEACH + " " + holidays);
		} catch (TwitterException err) {
			loadingMsg.delete();
			Utils.handle("getting holidays information", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show the number of remaining days before the next school holidays for the indicated zone.")
				.addArg(Zone.values(), false)
				.build();
	}

}
