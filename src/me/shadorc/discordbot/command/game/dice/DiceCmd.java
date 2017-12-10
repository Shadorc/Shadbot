package me.shadorc.discordbot.command.game.dice;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.game.GameUtils;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends AbstractCommand {

	protected static final int MULTIPLIER = 5;
	private static final int MAX_BET = 100_000;

	public DiceCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "dice");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(DiceManager.CHANNELS_DICE.containsKey(context.getChannel().getLongID())) {
			this.joinGame(context);
		} else {
			this.createGame(context);
		}
	}

	private void createGame(Context context) throws MissingArgumentException {
		String[] splitArgs = StringUtils.getSplittedArg(context.getArg());
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		Integer bet = GameUtils.parseBetOrWarn(splitArgs[0], MAX_BET, context);
		if(bet == null) {
			return;
		}

		String numStr = splitArgs[1];
		if(!StringUtils.isIntBetween(numStr, 1, 6)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 6.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);

		DiceManager diceManager = DiceManager.CHANNELS_DICE.getOrDefault(context.getChannel().getLongID(), new DiceManager(context, bet));

		DiceManager currentManager = DiceManager.CHANNELS_DICE.putIfAbsent(context.getChannel().getLongID(), diceManager);
		if(currentManager == null) {
			diceManager.start();
		} else {
			diceManager = currentManager;
		}

		diceManager.addPlayer(context.getAuthor(), num);
		BotUtils.sendMessage(Emoji.DICE + " **" + context.getAuthorName() + "** bets on **" + num + "**.", context.getChannel());
	}

	private void joinGame(Context context) {
		DiceManager diceManager = DiceManager.CHANNELS_DICE.get(context.getChannel().getLongID());
		if(DatabaseManager.getCoins(context.getGuild(), context.getAuthor()) < diceManager.getBet()) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		if(diceManager.isPlaying(context.getAuthor())) {
			BotUtils.sendMessage(Emoji.INFO + " You're already participating.", context.getChannel());
			return;
		}

		if(diceManager.getPlayers() == 6) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry, there are already 6 players.", context.getChannel());
			return;
		}

		String numStr = context.getArg();
		if(!StringUtils.isIntBetween(numStr, 1, 6)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 6.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);

		if(diceManager.isBet(num)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This number has already been bet, please try with another one.", context.getChannel());
			return;
		}

		diceManager.addPlayer(context.getAuthor(), num);
		BotUtils.sendMessage(Emoji.DICE + " **" + context.getAuthorName() + "** bets on **" + num + "**.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a dice game with a common bet or join a game in progress.**")
				.appendField("Usage", "**Create a game:** `" + context.getPrefix() + "dice <bet> <num>`"
						+ "\n**Join a game:** `" + context.getPrefix() + this.getFirstName() + " <num>`", false)
				.appendField("Restrictions", "**num** - must be between 1 and 6"
						+ "\nYou can't bet on a number that has already been chosen by another player.", false)
				.appendField("Gains", "The winner gets the common bet multiplied by " + MULTIPLIER + " plus the number of players."
						+ "\ngains = bet * (" + MULTIPLIER + " + players)", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
