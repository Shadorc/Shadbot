package me.shadorc.shadbot.command.game.rps;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RpsCmd extends BaseCmd {

	private static final int GAINS = 500;

	private final Map<Tuple2<Snowflake, Snowflake>, AtomicInteger> winStreaks;

	public RpsCmd() {
		super(CommandCategory.GAME, List.of("rps"));
		this.setGameRateLimiter();
		this.winStreaks = new ConcurrentHashMap<>();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Handsign userHandsign = Utils.parseEnum(Handsign.class, arg,
				new CommandException(String.format("`%s` is not a valid handsign. %s.",
						arg, FormatUtils.options(Handsign.class))));

		final Handsign botHandsign = Utils.randValue(Handsign.values());

		final StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s %s **VS** %s %s :**Shadbot**%n",
				context.getUsername(), userHandsign.getHandsign(), userHandsign.getEmoji(),
				botHandsign.getEmoji(), botHandsign.getHandsign()));

		final AtomicInteger userCombo = this.getCombo(context);
		if(userHandsign.isSuperior(botHandsign)) {
			userCombo.incrementAndGet();
			final int gains = GAINS * userCombo.get();
			strBuilder.append(String.format(Emoji.BANK + " (**%s**) Well done, you won **%d coins** (Win Streak x%d)!",
					context.getUsername(), gains, userCombo.get()));
			Shadbot.getDatabase().getDBMember(context.getGuildId(), context.getAuthorId()).addCoins(gains);
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getName(), gains);
		} else if(userHandsign.equals(botHandsign)) {
			userCombo.set(0);
			strBuilder.append("It's a draw.");
		} else {
			userCombo.set(0);
			strBuilder.append("I won !");
		}
		this.getCombo(context).set(userCombo.get());

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.then();
	}

	private AtomicInteger getCombo(Context context) {
		return this.winStreaks.computeIfAbsent(Tuples.of(context.getGuildId(), context.getAuthorId()),
				ignored -> new AtomicInteger(0));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play a Rock–paper–scissors game.")
				.addArg("handsign", FormatUtils.format(Handsign.values(), Handsign::getHandsign, ", "), false)
				.setGains("The winner gets **%d coins**.", GAINS)
				.build();
	}

}
