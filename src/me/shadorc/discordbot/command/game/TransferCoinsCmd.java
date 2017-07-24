package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
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
			BotUtils.sendMessage("Indiquez l'utilisateur et le montant à transférer : /transfert <montant> <utilisateur>", context.getChannel());
			return;
		}

		String[] splitCmd = context.getArg().split(" ", 2);
		if(splitCmd.length != 2) {
			BotUtils.sendMessage("Indiquez l'utilisateur et le montant à transférer : /transfert <montant> <utilisateur>", context.getChannel());
			return;
		}

		if(context.getMessage().getMentions().size() != 1) {
			BotUtils.sendMessage("Vous devez mentionner un utilisateur : /transfert <montant> <utilisateur>", context.getChannel());
			return;
		}

		try {
			int coins = Integer.parseInt(splitCmd[0]);
			IUser user = context.getMessage().getMentions().get(0);

			if(coins <= 0) {
				BotUtils.sendMessage("Vous devez transférer un montant supérieur à 0.", context.getChannel());
				return;
			}

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

			BotUtils.sendMessage(context.getAuthor().mention() + " a transféré " + coins + " coins à " + user.mention(), context.getChannel());
		} catch(NumberFormatException e1) {
			BotUtils.sendMessage("Montant invalide.", context.getChannel());
		}
	}
}
