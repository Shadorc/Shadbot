package me.shadorc.discordbot.command.game;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class RussianRouletteCmd extends Command {

	private static final int GAIN = 25;

	public RussianRouletteCmd() {
		super(false, "roulette_russe", "russian_roulette");
	}

	@Override
	public void execute(Context context) {
		if(Utils.rand(6) == 0) {
			BotUtils.sendMessage(Emoji.DICE + " Une goutte de sueur coule sur votre front, vous pressez la détente... **PAN** ... "
					+ "Désolé, vous êtes mort, vous perdez tous vos gains.", context.getChannel());
			Storage.storeCoins(context.getGuild(), context.getAuthor(), 0);
		} else {
			BotUtils.sendMessage(Emoji.DICE + " Une goutte de sueur coule sur votre front, vous pressez la détente... **click** ... "
					+ "Pfiou, vous êtes toujours en vie, vous remportez " + GAIN + " coins !", context.getChannel());
			Utils.addCoins(context.getGuild(), context.getAuthor(), GAIN);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Joue à la roulette russe.**")
				.appendField("Gains", "Vous avez 5/6 chance de gagner " + GAIN + " coins et 1/6 de perdre la totalité de vos coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
