package me.shadorc.shadbot.command.game.rps;

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
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "rps" })
public class RpsCmd extends AbstractCommand {

	private static final int GAINS = 170;

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Handsign userHandsign = Utils.getEnum(Handsign.class, arg);
		if(userHandsign == null) {
			throw new CommandException(String.format("`%s` is not a valid handsign. %s.", arg, FormatUtils.formatOptions(Handsign.class)));
		}

		final Handsign botHandsign = Utils.randValue(Handsign.values());

		StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s.%n**Shadbot**: %s.%n",
				context.getUsername(), userHandsign.getRepresentation(), botHandsign.getRepresentation()));

		if(userHandsign.equals(botHandsign)) {
			strBuilder.append("It's a draw !");
		} else if(userHandsign.isSuperior(botHandsign)) {
			strBuilder.append(String.format(Emoji.BANK + " Well done, you won **%d coins**.", context.getUsername(), GAINS));
			DatabaseManager.getDBMember(context.getGuildId(), context.getAuthorId()).addCoins(GAINS);
			MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getName(), GAINS);
		} else {
			strBuilder.append("I win !");
		}

		return BotUtils.sendMessage(strBuilder.toString(), context.getChannel()).then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play a Rock–paper–scissors game.")
				.addArg("handsign", FormatUtils.format(Handsign.values(), Handsign::getHandsign, ", "), false)
				.setGains("The winner gets **%d coins**.", GAINS)
				.build();
	}

}
