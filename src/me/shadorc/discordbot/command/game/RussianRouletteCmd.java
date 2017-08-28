package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class RussianRouletteCmd extends AbstractCommand {

	private static final float WIN_RATE = 0.30f;
	private static final float LOSE_RATE = 0.55f;
	private static final int MIN_COINS_GAINED = 10;

	private final RateLimiter rateLimiter;

	public RussianRouletteCmd() {
		super(Role.USER, "russian_roulette", "russian-roulette", "russianroulette");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("You can use the russian roulette only once every " + rateLimiter.getTimeout() + " seconds.", context);
			}
			return;
		}

		long userCoins = context.getPlayer().getCoins();
		if(MathUtils.rand(6) == 0) {
			long loss = (long) Math.ceil(-userCoins * LOSE_RATE);
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **PAN** ... "
					+ "Sorry, you died, you lose **" + Math.abs(loss) + " coins**.", context.getChannel());
			context.getPlayer().addCoins(loss);
		} else {
			long gain = (long) Math.max(MIN_COINS_GAINED, Math.ceil(userCoins * WIN_RATE));
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **click** ... "
					+ "Phew, you are still alive, you gets **" + gain + " coins** !", context.getChannel());
			context.getPlayer().addCoins(gain);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play russian roulette.**")
				.appendField("Gains", "You have 5-in-6 chance to win " + (int) (WIN_RATE * 100) + "% of your coins "
						+ "and a 1-in-6 chance to lose " + (int) (LOSE_RATE * 100) + "% of your coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
