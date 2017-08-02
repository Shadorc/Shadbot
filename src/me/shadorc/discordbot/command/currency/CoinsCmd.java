package me.shadorc.discordbot.command.currency;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class CoinsCmd extends Command {

	public CoinsCmd() {
		super(false, "coins", "coin");
	}

	@Override
	public void execute(Context context) {
		if(context.getMessage().getMentions().isEmpty()) {
			BotUtils.sendMessage(Emoji.PURSE + " Vous avez **" + context.getUser().getCoins() + " coin(s)**.", context.getChannel());
		}

		else {
			IUser user = context.getMessage().getMentions().get(0);
			int coins = Storage.getUser(context.getGuild(), user).getCoins();
			BotUtils.sendMessage(Emoji.PURSE + " " + user.getName() + " a **" + coins + " coin(s)**.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche le nombre de coins que vous possédez.\nPour connaître le nombre de coins d'un autre utilisateur, mentionnez le.**")
				.appendField("Utilisation", "/coins ou /coins <@user>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}