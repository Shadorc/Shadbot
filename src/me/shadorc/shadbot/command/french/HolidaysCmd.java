package me.shadorc.shadbot.command.french;

import java.io.IOException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.twitter.Twitter;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "vacs", "vacances" })
public class HolidaysCmd extends AbstractCommand {

	private static final Twitter TWITTER = new Twitter(APIKeys.get(APIKey.TWITTER_API_KEY),
			APIKeys.get(APIKey.TWITTER_API_SECRET));

	private enum Zone {
		A, B, C;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Zone zone = Utils.getEnum(Zone.class, arg);
		if(zone == null) {
			throw new CommandException(String.format("`%s` is not a valid zone. %s", arg, FormatUtils.options(Zone.class)));
		}

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final String holidays = StringUtils.remove(TWITTER.getLastTweet("Vacances_Zone" + zone), "#");
			return loadingMsg.send(String.format(Emoji.BEACH + " (**%s**) %s", context.getUsername(), holidays)).then();
		} catch (IOException err) {
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
