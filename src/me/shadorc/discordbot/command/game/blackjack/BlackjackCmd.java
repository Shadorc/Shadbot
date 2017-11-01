package me.shadorc.discordbot.command.game.blackjack;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.game.GameUtils;
import sx.blah.discord.util.EmbedBuilder;

public class BlackjackCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public BlackjackCmd() {
		super(CommandCategory.GAME, Role.USER, "blackjack", "bj");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Integer bet = GameUtils.parseBetOrWarn(context.getArg(), context);
		if(bet == null) {
			return;
		}

		BlackjackManager blackjackManager;
		if(BlackjackManager.CHANNELS_BLACKJACK.containsKey(context.getChannel().getLongID())) {
			blackjackManager = BlackjackManager.CHANNELS_BLACKJACK.get(context.getChannel().getLongID());
		} else {
			blackjackManager = new BlackjackManager(context);
			blackjackManager.start();
		}

		if(blackjackManager.isPlaying(context.getAuthor())) {
			BotUtils.sendMessage(Emoji.INFO + " You're already participating.", context.getChannel());
			return;
		}

		blackjackManager.addPlayer(context.getAuthor(), bet);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start or join a blackjack game.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <bet>`", false)
				.appendField("Info", "**double down** -  increase the initial bet by 100% in exchange for committing to stand"
						+ " after receiving exactly one more card", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
