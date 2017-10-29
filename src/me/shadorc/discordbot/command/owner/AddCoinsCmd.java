package me.shadorc.discordbot.command.owner;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class AddCoinsCmd extends AbstractCommand {

	public AddCoinsCmd() {
		super(CommandCategory.OWNER, Role.OWNER, "add_coins");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String coinsStr = context.getArg();
		if(!StringUtils.isPositiveInt(coinsStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid amount.", context.getChannel());
			return;
		}

		int coins = Integer.parseInt(coinsStr);
		Storage.addCoins(context.getGuild(), context.getAuthor(), coins);
		BotUtils.sendMessage(Emoji.CHECK_MARK + " You received **" + StringUtils.pluralOf(coins, "coin") + "**.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Add coins to your wallet.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getNames()[0] + " <coins>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
