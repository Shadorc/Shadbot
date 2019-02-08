package me.shadorc.shadbot.command.currency;

import java.util.Comparator;
import java.util.function.Consumer;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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
				.flatMap(dbMember -> Mono.zip(context.getClient().getUserById(dbMember.getId()).map(User::getUsername), Mono.just(dbMember.getCoins())))
				.collectList()
				.map(list -> {
					if(list.isEmpty()) {
						return "\nEveryone is poor here.";
					}
					return FormatUtils.numberedList(10, list.size(),
							count -> {
								final Tuple2<String, Integer> tuple = list.get(count - 1);
								return String.format("%d. **%s** - %s", count, tuple.getT1(), FormatUtils.coins(tuple.getT2()));
							});
				})
				.map(description -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> embed.setAuthor("Leaderboard", null, context.getAvatarUrl())
							.setDescription(description)))
				.flatMap(embed -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show coins leaderboard for this server.")
				.build();
	}
}
