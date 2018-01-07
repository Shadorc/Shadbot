package me.shadorc.shadbot.command.game.rps;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "rps" })
public class RpsCmd extends AbstractCommand {

	private static final int GAINS = 170;

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Handsign userHandsign = Utils.getValueOrNull(Handsign.class, context.getArg());
		if(userHandsign == null) {
			throw new IllegalCmdArgumentException("Invalid handsign, use `rock`, `paper` or `scissors`.");
		}

		Handsign botHandsign = Handsign.values()[MathUtils.rand(Handsign.values().length)];

		StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s.%n**Shadbot**: %s.%n",
				context.getAuthorName(), userHandsign.getRepresentation(), botHandsign.getRepresentation()));

		if(userHandsign.equals(botHandsign)) {
			strBuilder.append("It's a draw !");
		} else if(userHandsign.isSuperior(botHandsign)) {
			strBuilder.append(String.format("%s wins ! Well done, you won **%d coins**.", context.getAuthorName(), GAINS));
			Database.getDBUser(context.getGuild(), context.getAuthor()).addCoins(GAINS);
			// StatsManager.increment(this.getFirstName(), GAINS);
		} else {
			strBuilder.append(context.getClient().getOurUser().getName() + " wins !");
		}

		BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Play a Rock–paper–scissors game.")
				.addArg("handsign", FormatUtils.format(Handsign.values(), Handsign::getHandsign, ", "), false)
				.setGains("The winner gets **%d coins**.", GAINS)
				.build();
	}

}
