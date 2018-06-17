package me.shadorc.shadbot.command.french;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TwitterUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;
import twitter4j.TwitterException;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private enum Zone {
		A, B, C;
	}

	@Override
	public void execute(Context context) {
		context.requireArg();

		Zone zone = Utils.getValueOrNull(Zone.class, context.getArg().get());
		if(zone == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid zone. %s",
					context.getArg(), FormatUtils.formatOptions(Zone.class)));
		}

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String holidays = StringUtils.remove(TwitterUtils.getLastTweet("Vacances_Zone" + zone), "#");
			loadingMsg.send(Emoji.BEACH + " " + holidays);
		} catch (TwitterException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting holidays information", context, err.getCause()));
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show the number of remaining days before the next school holidays for the indicated zone.")
				.addArg(Zone.values(), false)
				.build();
	}

}
