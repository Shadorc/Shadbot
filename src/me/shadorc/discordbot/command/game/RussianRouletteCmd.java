package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RussianRouletteCmd extends Command {

	private static final int GAIN = 25;

	public RussianRouletteCmd() {
		super(false, "roulette_russe", "russian_roulette");
	}

	@Override
	public void execute(Context context) {
		if(MathUtils.rand(6) == 0) {
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **PAN** ... "
					+ "Sorry, you died, you lose all your coins.", context.getChannel());
			context.getUser().setCoins(0);
		} else {
			BotUtils.sendMessage(Emoji.DICE + " You break a sweat, you pull the trigger... **click** ... "
					+ "Phew, you are still alive, you gets " + GAIN + " coins !", context.getChannel());
			context.getUser().addCoins(GAIN);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play russian roulette.**")
				.appendField("Gains", "You have 5-in-6 chance to win " + GAIN + " coins and a 1-in-6 chance to lose all your coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
