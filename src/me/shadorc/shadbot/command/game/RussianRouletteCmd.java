package me.shadorc.shadbot.command.game;

import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "russian_roulette", "russian-roulette", "russianroulette" }, alias = "rr")
public class RussianRouletteCmd extends AbstractCommand {

	private static final int MAX_BET = 250_000;
	private static final float WIN_MULTIPLIER = 2.03f;
	private static final float LOSE_MULTIPLIER = 10f;

	@Override
	public void execute(Context context) {
		final String arg = context.requireArg();

		context.getMember().subscribe(member -> {
			Integer bet = Utils.checkAndGetBet(context.getChannel(), member, arg, MAX_BET);
			if(bet == null) {
				return;
			}

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

			Database.getDBMember(context.getGuildId().get(), context.getAuthorId()).addCoins(gains);
			BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Play Russian roulette.")
				.addArg("bet", String.format("You can't bet more than **%s**.", FormatUtils.formatCoins(MAX_BET)), false)
				.setGains("You have a **5-in-6** chance to win **%.1f times** your bet and a **1-in-6** chance to lose **%.1f times** your bet.",
						WIN_MULTIPLIER, LOSE_MULTIPLIER)
				.build();
	}
}
