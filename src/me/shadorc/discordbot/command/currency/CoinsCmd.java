package me.shadorc.discordbot.command.currency;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class CoinsCmd extends AbstractCommand {

	public CoinsCmd() {
		super(CommandCategory.CURRENCY, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "coins", "coin");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getMessage().getMentions().isEmpty()) {
			BotUtils.sendMessage(Emoji.PURSE + " You have **"
					+ StringUtils.pluralOf(DatabaseManager.getCoins(context.getGuild(), context.getAuthor()), "coin")
					+ "**.", context.getChannel());
		} else {
			IUser user = context.getMessage().getMentions().get(0);
			int coins = DatabaseManager.getUser(context.getGuild(), user).getInt(DatabaseManager.COINS);
			BotUtils.sendMessage(Emoji.PURSE + " " + user.getName() + " has **" + StringUtils.pluralOf(coins, "coin") + "**.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show how much coins you or another user have.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<@user>]`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}