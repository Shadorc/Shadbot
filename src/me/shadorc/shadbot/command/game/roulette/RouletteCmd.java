package me.shadorc.shadbot.command.game.roulette;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class RouletteCmd extends GameCmd<RouletteManager> {

	public enum Place {
		RED, BLACK, ODD, EVEN, LOW, HIGH;
	}

	private static final int MAX_BET = 250_000;

	public RouletteCmd() {
		super(List.of("roulette"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final int bet = Utils.requireBet(context.getMember(), args.get(0), MAX_BET);

		final String place = args.get(1).toLowerCase();

		// Match [1-36], red, black, odd, even, high or low
		if(!place.matches("^([1-9]|1[0-9]|2[0-9]|3[0-6])$") && Utils.getEnum(Place.class, place) == null) {
			throw new CommandException(String.format("`%s` is not a valid place, must be a number between **1 and 36**, %s.",
					place, FormatUtils.format(Place.values(), value -> String.format("**%s**", StringUtils.toLowerCase(value)), ", ")));
		}

		final RouletteManager rouletteManager = this.getManagers().computeIfAbsent(context.getChannelId(),
				channelId -> {
					final RouletteManager manager = new RouletteManager(this, context);
					manager.start();
					return manager;
				});

		if(rouletteManager.addPlayer(context.getAuthorId(), bet, place)) {
			return rouletteManager.show();
		} else {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
							context.getUsername()), channel))
					.then();
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play a roulette game in which everyone can participate.")
				.addArg("bet", false)
				.addArg("place", String.format("number between 1 and 36, %s", FormatUtils.format(Place.class, ", ")), false)
				.addField("Info", "**low** - numbers between 1 and 18"
						+ "\n**high** - numbers between 19 and 36", false)
				.build();
	}
}
