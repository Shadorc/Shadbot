package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;

public class CoinsCmd extends Command {

	public CoinsCmd() {
		super(false, "coins", "coin");
	}

	@Override
	public void execute(Context context) {
		String coinsStr = Storage.get(context.getGuild(), context.getAuthor().getLongID()).toString();
		int coins = coinsStr == null ? 0 : Integer.parseInt(coinsStr);
		BotUtils.sendMessage("Vous avez " + coins + " coins.", context.getChannel());
	}
}