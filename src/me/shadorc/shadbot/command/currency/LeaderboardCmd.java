package me.shadorc.shadbot.command.currency;

import java.util.Comparator;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.database.DBMember;
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

	private static final Comparator<DBMember> COMPARATOR =
			(user1, user2) -> Integer.compare(user1.getCoins(), user2.getCoins());

	@Override
	public Mono<Void> execute(Context context) {
		return Flux.fromIterable(Shadbot.getDatabase().getDBGuild(context.getGuildId()).getMembers())
				.filter(dbMember -> dbMember.getCoins() > 0)
				.sort(COMPARATOR.reversed())
				.take(10)
				.flatMap(dbMember -> context.getClient().getUserById(dbMember.getId())
						.zipWith(Mono.just(dbMember.getCoins())))
				.collectList()
				.map(list -> {
					if(list.isEmpty()) {
						return "\nEveryone is poor here.";
					}
					return FormatUtils.numberedList(10, list.size(),
							count -> {
								final Tuple2<User, Integer> userAndCoins = list.get(count - 1);
								final String username = userAndCoins.getT1().getUsername();
								final String coins = FormatUtils.coins(userAndCoins.getT2());
								return String.format("%d. **%s** - %s", count, username, coins);
							});
				})
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
