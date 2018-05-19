package me.shadorc.shadbot.command.currency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "leaderboard" })
public class LeaderboardCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Map<String, Integer> unsortedUsersMap = new HashMap<>();

		for(DBMember dbUser : Database.getDBGuild(context.getGuild().get()).getUsers()) {
			int userCoin = dbUser.getCoins();
			if(userCoin > 0) {
				context.getClient().getMemberById(context.getGuildId(), dbUser.getId())
						.blockOptional()
						.ifPresent(user -> unsortedUsersMap.put(user.getUsername(), userCoin));
			}
		}

		Map<String, Integer> sortedUsersMap = Utils.sortByValue(unsortedUsersMap);
		List<String> usersList = new ArrayList<>(sortedUsersMap.keySet());

		String leaderboard = FormatUtils.numberedList(10, sortedUsersMap.size(),
				count -> String.format("%d. **%s** - %s",
						count,
						usersList.get(count - 1),
						FormatUtils.formatCoins(sortedUsersMap.get(usersList.get(count - 1)))));

		if(leaderboard.isEmpty()) {
			leaderboard = "\nEveryone is poor here.";
		}

		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Leaderboard")
				.setDescription(leaderboard);

		BotUtils.sendMessage(embed, context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show coins leaderboard for this server.")
				.build();
	}
}
