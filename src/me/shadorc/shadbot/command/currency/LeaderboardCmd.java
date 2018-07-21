package me.shadorc.shadbot.command.currency;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "leaderboard" })
public class LeaderboardCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		return Flux.fromIterable(DatabaseManager.getDBGuild(context.getGuildId()).getMembers())
				.filter(dbMember -> dbMember.getCoins() > 0)
				.sort((user1, user2) -> Integer.compare(user1.getCoins(), user2.getCoins()))
				.take(10)
				.flatMap(dbMember -> context.getClient().getUserById(dbMember.getId())
						.zipWith(Mono.just(dbMember.getCoins())))
				.collectList()
				.map(list -> FormatUtils.numberedList(10, list.size(),
						count -> {
							final Tuple2<User, Integer> userAndCoins = list.get(count - 1);
							final String username = userAndCoins.getT1().getUsername();
							final String coins = FormatUtils.formatCoins(userAndCoins.getT2());
							return String.format("%d. **%s** - %s", count, username, coins);
						}))
				.defaultIfEmpty("\nEveryone is poor here.")
				.zipWith(context.getAvatarUrl())
				.map(msgAndAvatar -> EmbedUtils.getDefaultEmbed()
						.setAuthor("Leaderboard", null, msgAndAvatar.getT2())
						.setDescription(msgAndAvatar.getT1()))
				.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show coins leaderboard for this server.")
				.build();
	}
}
