package me.shadorc.discordbot.command.currency;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class CoinsCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public CoinsCmd() {
		super(CommandCategory.CURRENCY, Role.USER, "coins", "coin");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(context.getMessage().getMentions().isEmpty()) {
			BotUtils.sendMessage(Emoji.PURSE + " You have **" + StringUtils.pluralOf(context.getPlayer().getCoins(), "coin") + "**.", context.getChannel());
		} else {
			IUser user = context.getMessage().getMentions().get(0);
			int coins = Storage.getPlayer(context.getGuild(), user).getCoins();
			BotUtils.sendMessage(Emoji.PURSE + " " + user.getName() + " has **" + StringUtils.pluralOf(coins, "coin") + "**.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show how many coins you have.\nYou can also see how much coins have an user by mentioning him.**")
				.appendField("Usage", "`" + context.getPrefix() + "coins`"
						+ "\n`" + context.getPrefix() + "coins <@user>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}