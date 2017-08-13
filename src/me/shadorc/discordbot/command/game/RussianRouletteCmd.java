package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RussianRouletteCmd extends Command {

	private final RateLimiter rateLimiter;

	public RussianRouletteCmd() {
		super(false, "russian_roulette", "roulette_russe");
		this.rateLimiter = new RateLimiter(10, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("You can use the russian roulette only once every " + rateLimiter.getTimeout() + " seconds.", context);
			}
			return;
		}

		int userCoins = context.getUser().getCoins();
		if(MathUtils.rand(6) == 0) {
			int loss = (int) Math.ceil(-userCoins * 0.50);
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **PAN** ... "
					+ "Sorry, you died, you lose **" + Math.abs(loss) + " coins**.", context.getChannel());
			context.getUser().addCoins(loss);
		} else {
			int gain = (int) Math.ceil(userCoins * 0.30);
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **click** ... "
					+ "Phew, you are still alive, you gets **" + gain + " coins** !", context.getChannel());
			context.getUser().addCoins(gain);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play russian roulette.**")
				.appendField("Gains", "You have 5-in-6 chance to win 30% of your coins and a 1-in-6 chance to lose 50% of your coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
