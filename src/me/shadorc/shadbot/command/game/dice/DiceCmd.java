package me.shadorc.shadbot.command.game.dice;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class DiceCmd extends GameCmd<DiceGame> {

	protected static final float MULTIPLIER = 4.5f;
	private static final int MAX_BET = 250_000;

	public DiceCmd() {
		super(List.of("dice"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, 2);

		// This boolean indicates if the user is trying to join or to create a game
		final boolean isJoining = args.size() == 1;

		final Integer number = NumberUtils.asIntBetween(args.get(0), 1, 6);
		if(number == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid number, must be between 1 and 6.",
					args.get(0))));
		}

		// A game is already started...
		if(this.getManagers().containsKey(context.getChannelId())) {
			// ... and the user is trying to create a game
			if(!isJoining) {
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO
								+ " (**%s**) A **Dice Game** has already been started. "
								+ "Use `%s%s <num>` to join it.",
								context.getUsername(), context.getPrefix(), this.getName()), channel))
						.then();
			}

			final DiceGame diceManager = this.getManagers().get(context.getChannelId());

			if(diceManager.getPlayers().containsKey(context.getAuthorId())) {
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO
								+ " (**%s**) You're already participating.",
								context.getUsername()), channel))
						.then();
			}

			if(diceManager.getPlayers().size() == 6) {
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION
								+ " (**%s**) Sorry, there are already 6 players.",
								context.getUsername()), channel))
						.then();
			}

			if(diceManager.getPlayers().values().stream().anyMatch(player -> player.getNumber() == number)) {
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION
								+ " (**%s**) This number has already been bet, please try with another one.",
								context.getUsername()), channel))
						.then();
			}

			Utils.requireBet(context.getMember(), Integer.toString(diceManager.getBet()), MAX_BET);
			diceManager.addPlayerIfAbsent(new DicePlayer(context.getAuthorId(), number));
			return diceManager.show();
		}
		// A game is not already started...
		else {
			// ... and the user tries to join a game
			if(isJoining) {
				return Mono.error(new MissingArgumentException());
			}

			final Integer bet = Utils.requireBet(context.getMember(), args.get(1), MAX_BET);
			final DiceGame diceManager = this.getManagers().computeIfAbsent(context.getChannelId(),
					ignored -> new DiceGame(this, context, bet));
			diceManager.addPlayerIfAbsent(new DicePlayer(context.getAuthorId(), number));
			diceManager.start();
			return diceManager.show();
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start a dice game with a common bet.")
				.addArg("num", "number between 1 and 6\nYou can't bet on a number that has already been chosen by another player.", false)
				.addArg("bet", false)
				.setGains("The winner gets the prize pool plus %.1f times his bet", MULTIPLIER)
				.build();
	}
}
