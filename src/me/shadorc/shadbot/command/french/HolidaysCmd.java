package me.shadorc.shadbot.command.french;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TwitterUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import twitter4j.TwitterException;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private enum Zone {
		A, B, C;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Zone zone = Utils.getValueOrNull(Zone.class, arg);
		if(zone == null) {
			throw new CommandException(String.format("`%s` is not a valid zone. %s", arg, FormatUtils.formatOptions(Zone.class)));
		}

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final String holidays = StringUtils.remove(TwitterUtils.getLastTweet("Vacances_Zone" + zone), "#");
			return context.getAuthorName()
					.flatMap(username -> loadingMsg.send(String.format(Emoji.BEACH + " (**%s**) %s", username, holidays)))
					.then();
		} catch (TwitterException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
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
