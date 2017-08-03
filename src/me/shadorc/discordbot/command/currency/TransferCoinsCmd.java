package me.shadorc.discordbot.command.currency;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.rpg.User;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class TransferCoinsCmd extends Command {

	public TransferCoinsCmd() {
		super(false, "transfert", "transfer");
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
			User receiverUser = Storage.getUser(context.getGuild(), context.getMessage().getMentions().get(0));
			User senderUser = context.getUser();

			if(coins <= 0 || senderUser.equals(receiverUser)) {
				throw new IllegalArgumentException();
			}

			if(senderUser.getCoins() < coins) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour effectuer ce transfert.", context.getChannel());
				return;
			}

			senderUser.addCoins(-coins);
			receiverUser.addCoins(coins);

			BotUtils.sendMessage(Emoji.BANK + " " + senderUser.mention() + " a transféré " + coins + " coins à " + receiverUser.mention(), context.getChannel());
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
