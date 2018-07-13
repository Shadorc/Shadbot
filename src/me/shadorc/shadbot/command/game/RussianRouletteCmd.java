package me.shadorc.shadbot.command.game;

import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "russian_roulette", "russian-roulette", "russianroulette" }, alias = "rr")
public class RussianRouletteCmd extends AbstractCommand {

	private static final int MAX_BET = 250_000;
	private static final float WIN_MULTIPLIER = 2.03f;
	private static final float LOSE_MULTIPLIER = 10f;

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		return Utils.checkAndGetBet(context.getChannel(), context.getMember().get(), arg, MAX_BET)
				.map(bet -> {
					StringBuilder strBuilder = new StringBuilder(Emoji.DICE + " You break a sweat, you pull the trigger... ");

					int gains;
					if(ThreadLocalRandom.current().nextInt(6) == 0) {
						gains = (int) -Math.ceil(bet * LOSE_MULTIPLIER);
						MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getName(), Math.abs(gains));
						strBuilder.append(String.format("**PAN** ... Sorry, you died. You lose **%s**.", FormatUtils.formatCoins(Math.abs(gains))));
					} else {
						gains = (int) Math.ceil(bet * WIN_MULTIPLIER);
						MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getName(), gains);
						strBuilder.append(String.format("**click** ... Phew, you are still alive ! You get **%s**.", FormatUtils.formatCoins(gains)));
					}

					DatabaseManager.getDBMember(context.getGuildId().get(), context.getAuthorId()).addCoins(gains);
					return strBuilder;
				})
				.flatMap(strBuilder -> BotUtils.sendMessage(strBuilder.toString(), context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play Russian roulette.")
				.addArg("bet", String.format("You can't bet more than **%s**.", FormatUtils.formatCoins(MAX_BET)), false)
				.setGains("You have a **5-in-6** chance to win **%.1f times** your bet and a **1-in-6** chance to lose **%.1f times** your bet.",
						WIN_MULTIPLIER, LOSE_MULTIPLIER)
				.build();
	}
}
