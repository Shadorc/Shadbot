package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.utility.BotUtils;

public class CoinsCmd extends Command {

	public CoinsCmd() {
		super("coins", "coin");
	}

	@Override
	public void execute(Context context) {
		String coinsStr = Storage.get(context.getGuild(), context.getAuthor().getLongID());
		int coins = coinsStr == null ? 0 : Integer.parseInt(coinsStr);
		BotUtils.sendMessage("Vous avez " + coins + " coins.", context.getChannel());
	}
}