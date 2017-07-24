package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IUser;

public class TransferCoinsCmd extends Command {

	public TransferCoinsCmd() {
		super(false, "transfert", "transferer", "donner", "transfer");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Indiquez la personne et le montant à transférer : /transfert <montant> <utilisateur>", context.getChannel());
			return;
		}

		String[] splitCmd = context.getArg().split(" ", 2);
		if(splitCmd.length < 2) {
			BotUtils.sendMessage("Indiquez la personne et le montant à transférer : /transfert <montant> <utilisateur>", context.getChannel());
			return;
		}

		try {
			int coins = Integer.parseInt(splitCmd[0]);
			IUser user = context.getGuild().getUsersByName(splitCmd[1]).get(0);

			if(user.equals(context.getAuthor())) {
				BotUtils.sendMessage("Vous ne pouvez pas vous transférer de l'argent à vous même.", context.getChannel());
				return;
			}

			if(Integer.parseInt(Storage.get(context.getGuild(), context.getAuthor().getLongID()).toString()) < coins) {
				BotUtils.sendMessage("Vous n'avez pas assez de coins pour effectuer ce transfert.", context.getChannel());
				return;
			}

			Utils.gain(context.getGuild(), context.getAuthor().getLongID(), -coins);
			Utils.gain(context.getGuild(), user.getLongID(), coins);

			BotUtils.sendMessage(coins + " coins ont été transférés à " + user.getName(), context.getChannel());
		} catch(NumberFormatException e1) {
			BotUtils.sendMessage("Montant invalide.", context.getChannel());
		} catch(IndexOutOfBoundsException e2) {
			BotUtils.sendMessage("Utilisateur inconnu.", context.getChannel());
		}
	}
}
