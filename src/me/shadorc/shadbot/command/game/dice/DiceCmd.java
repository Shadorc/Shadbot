package me.shadorc.shadbot.command.game.dice;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 2)
@Command(category = CommandCategory.GAME, names = { "dice" })
public class DiceCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Snowflake, DiceManager> MANAGERS = new ConcurrentHashMap<>();

	protected static final float MULTIPLIER = 4.5f;
	private static final int MAX_BET = 250_000;

	@Override
	public Mono<Void> execute(Context context) {
		List<String> args = context.requireArgs(1, 2);

		// This value indicates if the user is trying to join or create a game
		final boolean isJoining = args.size() == 1;

		final String numStr = args.get(isJoining ? 0 : 1);
		Integer num = NumberUtils.asIntBetween(numStr, 1, 6);
		if(num == null) {
			throw new CommandException(String.format("`%s` is not a valid number, must be between 1 and 6.", numStr));
		}

		DiceManager diceManager = MANAGERS.get(context.getChannelId());

		// The user tries to join a game and no game are currently playing
		if(isJoining && diceManager == null) {
			throw new MissingArgumentException();
		}

		final String betStr = isJoining ? Integer.toString(diceManager.getBet()) : args.get(0);
		final Integer bet = Utils.requireBet(context.getMember(), betStr, MAX_BET);

		if(!isJoining) {
			// The user tries to start a game and it has already been started
			if(diceManager != null) {
				return BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) A **Dice Game** has already been started. "
						+ "Use `%s%s <num>` to join it.",
						context.getUsername(), context.getPrefix(), this.getName()), context.getChannel())
						.then();
			}

			diceManager = new DiceManager(context, bet);
		}

		if(MANAGERS.putIfAbsent(context.getChannelId(), diceManager) == null) {
			diceManager.start();
		}

		if(diceManager.getPlayersCount() == 6) {
			return BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) Sorry, there are already 6 players.",
					context.getUsername()), context.getChannel())
					.then();
		}

		if(diceManager.isNumBet(num)) {
			return BotUtils.sendMessage(
					String.format(Emoji.GREY_EXCLAMATION + " (**%s**) This number has already been bet, please try with another one.",
							context.getUsername()), context.getChannel())
					.then();
		}

		if(diceManager.addPlayer(context.getAuthorId(), num)) {
			return diceManager.show().then();
		} else {
			return BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
					context.getUsername()), context.getChannel())
					.then();
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start a dice game with a common bet.")
				.addArg("bet", false)
				.addArg("num", "number between 1 and 6\nYou can't bet on a number that has already been chosen by another player.", false)
				.setGains("The winner gets the prize pool plus %.1f times his bet", MULTIPLIER)
				.build();
	}
}
