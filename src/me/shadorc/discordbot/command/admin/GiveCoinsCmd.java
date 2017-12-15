package me.shadorc.discordbot.command.admin;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class GiveCoinsCmd extends AbstractCommand {

	public GiveCoinsCmd() {
		super(CommandCategory.ADMIN, Role.ADMIN, RateLimiter.DEFAULT_COOLDOWN, "give_coins");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String coinsStr = StringUtils.getSplittedArg(context.getArg())[0];
		if(!StringUtils.isPositiveInt(coinsStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid amount.", context.getChannel());
			return;
		}

		int coins = Integer.parseInt(coinsStr);

		StringBuilder strBuilder = new StringBuilder();
		if(context.getMessage().getMentions().isEmpty()) {
			DatabaseManager.addCoins(context.getChannel(), context.getAuthor(), coins);
			strBuilder.append("You");

		} else {
			for(IUser user : context.getMessage().getMentions()) {
				DatabaseManager.addCoins(context.getChannel(), user, coins);
			}
			strBuilder.append(FormatUtils.formatList(context.getMessage().getMentions(), user -> user.getName(), ", "));
		}
		BotUtils.sendMessage(Emoji.CHECK_MARK + " **" + strBuilder.toString() + "** received **" + FormatUtils.formatCoins(coins) + "**.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Give coins to an user.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <coins> [<@user(s)>]`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
