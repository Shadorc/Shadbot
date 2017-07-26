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
		int coins = Storage.getCoins(context.getGuild(), context.getAuthor());
		BotUtils.sendMessage(":purse: Vous avez " + coins + " coins.", context.getChannel());
	}
}