package me.shadorc.discordbot.command.currency;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class LeaderboardCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public LeaderboardCmd() {
		super(Role.USER, "leaderboard");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		Map<IUser, Integer> usersCoin = new HashMap<>();
		for(IUser user : context.getGuild().getUsers()) {
			int userCoin = Storage.getPlayer(context.getGuild(), user).getCoins();
			if(userCoin > 0) {
				usersCoin.put(user, userCoin);
			}
		}
		usersCoin = Utils.sortByValue(usersCoin);

		int count = 0;
		StringBuilder strBuilder = new StringBuilder();
		for(IUser user : usersCoin.keySet()) {
			if(count > 10) {
				break;
			}
			count++;
			strBuilder.append("\n" + count + ". **" + user.getName() + "** - " + usersCoin.get(user) + " coins");
		}

		if(count == 0) {
			strBuilder.append("\nEveryone is poor here.");
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Leaderboard")
				.appendDescription(strBuilder.toString());

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show coins leaderboard for this server.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
