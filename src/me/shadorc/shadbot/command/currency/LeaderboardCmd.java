package me.shadorc.shadbot.command.currency;

import java.util.List;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "leaderboard" })
public class LeaderboardCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.getGuild().subscribe(guild -> {
			Flux.fromStream(Database.getDBGuild(guild.getId()).getUsers().stream())
					.filter(dbMember -> dbMember.getCoins() > 0)
					.sort((user1, user2) -> Integer.compare(user1.getCoins(), user2.getCoins()))
					.take(10)
					.flatMap(dbMember -> context.getClient().getUserById(dbMember.getId())
							.map(member -> Tuples.of(member, dbMember.getCoins())))
					.buffer()
					.subscribe(list -> this.execute(context, list));
		});
	}

	private void execute(Context context, List<Tuple2<User, Integer>> list) {
		String leaderboard = FormatUtils.numberedList(10, list.size(),
				count -> String.format("%d. **%s** - %s",
						count,
						list.get(count - 1).getT1().getUsername(),
						FormatUtils.formatCoins(list.get(count - 1).getT2())));

		if(leaderboard.isEmpty()) {
			leaderboard = "\nEveryone is poor here.";
		}

		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Leaderboard")
				.setDescription(leaderboard);

		BotUtils.sendMessage(embed, context.getChannel());
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show coins leaderboard for this server.")
				.build();
	}
}
