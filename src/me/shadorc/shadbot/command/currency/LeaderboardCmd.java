package me.shadorc.shadbot.command.currency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.data.db.DBUser;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "leaderboard" })
public class LeaderboardCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Map<String, Integer> unsortedUsersMap = new HashMap<>();

		List<DBUser> users = Database.getDBGuild(context.getGuild()).getUsers();
		for(DBUser dbUser : users) {
			int userCoin = dbUser.getCoins();
			if(userCoin > 0) {
				IUser user = context.getGuild().getUserByID(dbUser.getUserID());
				if(user != null) {
					unsortedUsersMap.put(user.getName(), userCoin);
				}
			}
		}

		final Map<String, Integer> sortedUsersMap = Utils.sortByValue(unsortedUsersMap);
		List<String> usersList = new ArrayList<>(unsortedUsersMap.keySet());

		StringBuilder strBuilder = new StringBuilder();
		IntStream.rangeClosed(1, 10)
				.boxed()
				.limit(unsortedUsersMap.size())
				.forEach(count -> strBuilder.append(String.format("%n%d. **%d** - %s",
						count,
						usersList.get(count - 1),
						FormatUtils.formatCoins(sortedUsersMap.get(usersList.get(count - 1))))));

		if(strBuilder.length() == 0) {
			strBuilder.append("\nEveryone is poor here.");
		}

		EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Leaderboard")
				.appendDescription(strBuilder.toString());

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show coins leaderboard for this server.")
				.build();
	}
}
