package me.shadorc.discordbot.command.currency;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IUser;

public class CoinsCmd extends Command {

	public CoinsCmd() {
		super(false, "coins", "coin");
	}

	@Override
	public void execute(Context context) {
		if(context.getMessage().getMentions().isEmpty()) {
			int coins = Storage.getCoins(context.getGuild(), context.getAuthor());
			BotUtils.sendMessage(Emoji.PURSE + " Vous avez **" + coins + " coin(s)**.", context.getChannel());
		}

		else {
			IUser user = context.getMessage().getMentions().get(0);
			int coins = Storage.getCoins(context.getGuild(), user);
			BotUtils.sendMessage(Emoji.PURSE + " " + user.getName() + " a **" + coins + " coin(s)**.", context.getChannel());
		}
	}
}