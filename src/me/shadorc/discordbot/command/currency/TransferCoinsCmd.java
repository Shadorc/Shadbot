package me.shadorc.discordbot.command.currency;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TransferCoinsCmd extends Command {

	public TransferCoinsCmd() {
		super(false, "transfert", "transferer", "donner", "transfer");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		String[] splitCmd = context.getArg().split(" ", 2);
		if(splitCmd.length != 2 || context.getMessage().getMentions().size() != 1) {
			throw new IllegalArgumentException();
		}

		try {
			int coins = Integer.parseInt(splitCmd[0]);
			IUser user = context.getMessage().getMentions().get(0);

			if(coins <= 0 || user.equals(context.getAuthor())) {
				throw new IllegalArgumentException();
			}

			if(Storage.getCoins(context.getGuild(), context.getAuthor()) < coins) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour effectuer ce transfert.", context.getChannel());
				return;
			}

			Utils.addCoins(context.getGuild(), context.getAuthor(), -coins);
			Utils.addCoins(context.getGuild(), user, coins);

			BotUtils.sendMessage(Emoji.BANK + " " + context.getAuthor().mention() + " a transféré " + coins + " coins à " + user.mention(), context.getChannel());
		} catch(NumberFormatException e1) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Transfert des coins à l'utilisateur mentionné.**")
				.appendField("Utilisation", "/transfert <coins> <@utilisateur>", false)
				.appendField("Restrictions", "Le montant transféré doit être strictement supérieur à 0.\nVous ne pouvez pas vous transférer de coins à vous-même.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
