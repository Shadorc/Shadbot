package me.shadorc.discordbot.command.currency;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.JSONKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class LeaderboardCmd extends AbstractCommand {

	public LeaderboardCmd() {
		super(CommandCategory.CURRENCY, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "leaderboard");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Map<String, Integer> usersCoin = new HashMap<>();
		JSONObject usersObj = DatabaseManager.getUsers(context.getGuild());
		for(Object userID : usersObj.keySet()) {
			int userCoin = usersObj.getJSONObject(userID.toString()).getInt(JSONKey.COINS.toString());
			if(userCoin > 0) {
				IUser user = context.getGuild().getUserByID(Long.parseLong(userID.toString()));
				if(user != null) {
					usersCoin.put(user.getName(), userCoin);
				}
			}
		}
		usersCoin = Utils.sortByValue(usersCoin);

		int count = 0;
		StringBuilder strBuilder = new StringBuilder();
		for(String user : usersCoin.keySet()) {
			if(count > 10) {
				break;
			}
			count++;
			strBuilder.append("\n" + count + ". **" + user + "** - " + FormatUtils.formatCoins(usersCoin.get(user)));
		}

		if(count == 0) {
			strBuilder.append("\nEveryone is poor here.");
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Leaderboard")
				.appendDescription(strBuilder.toString());

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show coins leaderboard for this server.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
