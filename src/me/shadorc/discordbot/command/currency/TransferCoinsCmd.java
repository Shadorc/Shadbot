package me.shadorc.discordbot.command.currency;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Player;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class TransferCoinsCmd extends AbstractCommand {

	public TransferCoinsCmd() {
		super(Role.USER, "transfer");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitCmd = context.getArg().split(" ", 2);
		if(splitCmd.length != 2 || context.getMessage().getMentions().size() != 1) {
			throw new MissingArgumentException();
		}

		Player receiverPlayer = Storage.getPlayer(context.getGuild(), context.getMessage().getMentions().get(0));
		Player senderPlayer = context.getPlayer();
		if(senderPlayer.equals(receiverPlayer)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " You cannot transfer coins to yourself.", context.getChannel());
			return;
		}

		String coinsStr = splitCmd[0];
		if(!StringUtils.isPositiveLong(coinsStr)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid amount.", context.getChannel());
			return;
		}

		long coins = Long.parseLong(coinsStr);
		if(senderPlayer.getCoins() < coins) {
			BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins to do this.", context.getChannel());
			return;
		}

		senderPlayer.addCoins(-coins);
		receiverPlayer.addCoins(coins);

		BotUtils.sendMessage(Emoji.BANK + " " + senderPlayer.getUser().mention() + " has transfered "
				+ coins + " coins to " + receiverPlayer.getUser().mention(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Transfer coins to the mentioned user.**")
				.appendField("Usage", context.getPrefix() + "transfer <coins> <@user>", false)
				.appendField("Restriction", "**coins** - must be strictly positive", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
