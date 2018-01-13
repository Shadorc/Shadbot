package me.shadorc.shadbot.command.game.dice;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.GameUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 2)
@Command(category = CommandCategory.GAME, names = { "dice" })
public class DiceCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Long, DiceManager> MANAGERS = new ConcurrentHashMap<>();

	protected static final int MULTIPLIER = 5;
	private static final int MAX_BET = 100_000;

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(!MathUtils.isInRange(splitArgs.size(), 1, 2)) {
			throw new MissingArgumentException();
		}

		String numStr = splitArgs.get(splitArgs.size() == 1 ? 0 : 1);
		Integer num = CastUtils.asIntBetween(numStr, 1, 6);
		if(num == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid number, must be between 1 and 6.", numStr));
		}

		DiceManager diceManager = MANAGERS.get(context.getChannel().getLongID());

		// The user tries to join a game and no game are currently playing
		if(splitArgs.size() == 1 && diceManager == null) {
			throw new MissingArgumentException();
		}

		String betStr = splitArgs.size() == 1 ? Integer.toString(diceManager.getBet()) : splitArgs.get(0);
		Integer bet = GameUtils.checkAndGetBet(context.getChannel(), context.getAuthor(), betStr, MAX_BET);
		if(bet == null) {
			return;
		}

		if(splitArgs.size() == 2) {
			// The user tries to start a game and it has already been started
			if(diceManager != null) {
				BotUtils.sendMessage(String.format(Emoji.INFO + " A **Dice Game** has already been started. Use `%s%s <num>` to join it.",
						context.getPrefix(), this.getName()), context.getChannel());
				return;
			}

			diceManager = new DiceManager(this, context.getChannel(), context.getAuthor(), bet);
		}

		if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), diceManager) == null) {
			diceManager.start();
		}

		if(diceManager.getPlayersCount() == 6) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry, there are already 6 players.", context.getChannel());
			return;
		}

		if(diceManager.isNumBet(num)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This number has already been bet, please try with another one.", context.getChannel());
			return;
		}

		if(!diceManager.addPlayer(context.getAuthor(), num)) {
			BotUtils.sendMessage(Emoji.INFO + " You're already participating.", context.getChannel());
			return;
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Start a dice game with a common bet.")
				.addArg("bet", false)
				.addArg("num", "number between 1 and 6\nYou can't bet on a number that has already been chosen by another player.", false)
				.setGains("The winner gets the prize pool plus %d times his bet", MULTIPLIER)
				.build();
	}
}
