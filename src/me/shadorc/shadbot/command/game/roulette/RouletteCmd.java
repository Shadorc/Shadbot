package me.shadorc.shadbot.command.game.roulette;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "roulette" })
public class RouletteCmd extends AbstractCommand {

	public enum Place {
		RED, BLACK, ODD, EVEN, LOW, HIGH;
	}

	protected static final ConcurrentHashMap<Long, RouletteManager> MANAGERS = new ConcurrentHashMap<>();

	private static final int MAX_BET = 250_000;

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(context.getArg());
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Integer bet = Utils.checkAndGetBet(context.getChannel(), context.getAuthor(), splitArgs.get(0), MAX_BET);
		if(bet == null) {
			return;
		}

		String place = splitArgs.get(1).toLowerCase();
		// Match [1-36], red, black, odd, even, high or low
		if(!place.matches("^([1-9]|1[0-9]|2[0-9]|3[0-6])$") && Utils.getValueOrNull(Place.class, place) == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid place, must be a number between **1 and 36**, %s.",
					place, FormatUtils.format(Place.values(), value -> String.format("**%s**", value.toString().toLowerCase()), ", ")));
		}

		RouletteManager rouletteManager = MANAGERS.get(context.getChannel().getLongID());
		if(rouletteManager == null) {
			rouletteManager = new RouletteManager(this, context.getPrefix(), context.getChannel(), context.getAuthor());
		}

		if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), rouletteManager) == null) {
			rouletteManager.start();
		}

		if(!rouletteManager.addPlayer(context.getAuthor(), bet, place)) {
			BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
					context.getUsername()), context.getChannel());
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Play a roulette game in which everyone can participate.")
				.addArg("bet", false)
				.addArg("place", String.format("number between 1 and 36, %s",
						FormatUtils.format(Place.values(), value -> value.toString().toLowerCase(), ", ")), false)
				.addField("Info", "**low** - numbers between 1 and 18"
						+ "\n**high** - numbers between 19 and 36", false)
				.build();
	}
}
