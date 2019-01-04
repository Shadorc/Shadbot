package me.shadorc.shadbot.command.game.rps;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
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
			throw new CommandException(String.format("`%s` is not a valid handsign. %s.", arg, FormatUtils.options(Handsign.class)));
		}

		final Handsign botHandsign = Utils.randValue(Handsign.values());

		final StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s.%n**Shadbot**: %s.%n",
				context.getUsername(), userHandsign.getRepresentation(), botHandsign.getRepresentation()));

		if(userHandsign.equals(botHandsign)) {
			strBuilder.append("It's a draw !");
		} else if(userHandsign.isSuperior(botHandsign)) {
			strBuilder.append(String.format(Emoji.BANK + " (**%s**) Well done, you won **%d coins**.", context.getUsername(), GAINS));
			Shadbot.getDatabase().getDBMember(context.getGuildId(), context.getAuthorId()).addCoins(GAINS);
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getName(), GAINS);
		} else {
			strBuilder.append("I win !");
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.then();
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
