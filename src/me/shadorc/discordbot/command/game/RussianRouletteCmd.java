package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.game.GameUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RussianRouletteCmd extends AbstractCommand {

	private static final int MAX_BET = 200_000_000;
	private static final float WIN_MULTIPLIER = 2.25f;
	private static final float LOSE_MULTIPLIER = 10f;

	public RussianRouletteCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.GAME_COOLDOWN, "russian_roulette", "russian-roulette", "russianroulette");
		this.setAlias("rr");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Integer bet = GameUtils.parseBetOrWarn(context.getArg(), MAX_BET, context);
		if(bet == null) {
			return;
		}

		StringBuilder strBuilder = new StringBuilder(Emoji.DICE + " You break a sweat, you pull the trigger... ");

		int gains;
		if(MathUtils.rand(6) == 0) {
			gains = (int) -Math.ceil(bet * LOSE_MULTIPLIER);
			strBuilder.append("**PAN** ... Sorry, you died. You lose **" + FormatUtils.formatCoins(Math.abs(gains)) + "**.");
			LottoDataManager.addToPool(Math.abs(gains));
		} else {
			gains = (int) Math.ceil(bet * WIN_MULTIPLIER);
			strBuilder.append("**click** ... Phew, you are still alive ! You get **" + FormatUtils.formatCoins(gains) + "**.");
		}

		StatsManager.increment(gains > 0 ? StatCategory.MONEY_GAINS_COMMAND : StatCategory.MONEY_LOSSES_COMMAND, this.getFirstName(), Math.abs(gains));
		DatabaseManager.addCoins(context.getGuild(), context.getAuthor(), gains);
		BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play Russian roulette.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <bet>`", false)
				.appendField("Restriction", "**bet** - You can not bet more than **" + MAX_BET + " coins**.", false)
				.appendField("Gains", "You have a **5-in-6** chance to win **" + WIN_MULTIPLIER + " times** your bet and "
						+ "a **1-in-6** chance to lose **" + LOSE_MULTIPLIER + " times** your bet.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
