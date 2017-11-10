package me.shadorc.discordbot.command.currency;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class TransferCoinsCmd extends AbstractCommand {

	public TransferCoinsCmd() {
		super(CommandCategory.CURRENCY, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "transfer");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitCmd = StringUtils.getSplittedArg(context.getArg(), 2);
		if(splitCmd.length != 2 || context.getMessage().getMentions().size() != 1) {
			throw new MissingArgumentException();
		}

		IUser receiverUser = context.getMessage().getMentions().get(0);
		IUser senderUser = context.getAuthor();
		if(receiverUser.equals(senderUser)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You cannot transfer coins to yourself.", context.getChannel());
			return;
		}

		String coinsStr = splitCmd[0];
		if(!StringUtils.isPositiveInt(coinsStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid amount.", context.getChannel());
			return;
		}

		int coins = Integer.parseInt(coinsStr);
		if(DatabaseManager.getCoins(context.getGuild(), senderUser) < coins) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		if(DatabaseManager.getCoins(context.getGuild(), receiverUser) + coins >= Config.MAX_COINS) {
			BotUtils.sendMessage(Emoji.BANK + " This transfer cannot be done because " + receiverUser.getName()
					+ " would exceed the maximum coins cap.", context.getChannel());
			return;
		}

		DatabaseManager.addCoins(context.getGuild(), senderUser, -coins);
		DatabaseManager.addCoins(context.getGuild(), receiverUser, coins);

		BotUtils.sendMessage(Emoji.BANK + " " + senderUser.mention() + " has transfered **"
				+ coins + " coins** to " + receiverUser.mention(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Transfer coins to the mentioned user.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <coins> <@user>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
