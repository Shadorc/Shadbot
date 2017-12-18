package me.shadorc.discordbot.command.game.roulette;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.game.GameUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RouletteCmd extends AbstractCommand {

	private static final int MAX_BET = 100_000;

	public RouletteCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "roulette");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		String[] splitArgs = StringUtils.getSplittedArg(context.getArg());
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		Integer bet = GameUtils.parseBetOrWarn(splitArgs[0], MAX_BET, context);
		if(bet == null) {
			return;
		}

		String place = splitArgs[1].toLowerCase();
		// Match [1-36], red, black, odd, even, high or low
		if(!place.matches("^([1-9]|1[0-9]|2[0-9]|3[0-6])$|red|black|odd|even|high|low")) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid place, must be a number between **1 and 36**, "
					+ "**red**, **black**, **odd**, **even**, **low** or **high**.", context.getChannel());
			return;
		}

		RouletteManager rouletteManager = RouletteManager.CHANNELS_ROULETTE.get(context.getChannel().getLongID());
		if(rouletteManager == null) {
			rouletteManager = new RouletteManager(context);
		}

		if(RouletteManager.CHANNELS_ROULETTE.putIfAbsent(context.getChannel().getLongID(), rouletteManager) == null) {
			rouletteManager.start();
		}

		if(!rouletteManager.addPlayer(context.getAuthor(), bet, place)) {
			BotUtils.sendMessage(Emoji.INFO + " You're already participating.", context.getChannel());
			return;
		}

		BotUtils.sendMessage(Emoji.DICE + " **" + context.getAuthorName() + "** bets **" + FormatUtils.formatCoins(bet)
				+ "** on **" + place + "**.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play a roulette game in which everyone can participate.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <bet> <place>`", false)
				.appendField("Restrictions", "**place** - must be a number between 1 and 36, red, black, even, odd, low or high", false)
				.appendField("Info", "**low** - numbers between 1 and 18"
						+ "\n**high** - numbers between 19 and 36", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
