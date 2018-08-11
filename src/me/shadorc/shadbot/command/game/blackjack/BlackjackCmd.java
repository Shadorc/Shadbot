package me.shadorc.shadbot.command.game.blackjack;

import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAME, names = { "blackjack" }, alias = "bj")
public class BlackjackCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Snowflake, BlackjackManager> MANAGERS = new ConcurrentHashMap<>();

	private static final int MAX_BET = 250_000;

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Integer bet = Utils.requireBet(context.getMember(), arg, MAX_BET);

		BlackjackManager blackjackManager = MANAGERS.putIfAbsent(context.getChannelId(), new BlackjackManager(context));
		if(blackjackManager == null) {
			blackjackManager = MANAGERS.get(context.getChannelId());
			blackjackManager.start();
		}

		if(blackjackManager.addPlayerIfAbsent(context.getAuthorId(), bet)) {
			return blackjackManager.computeResultsOrShow();
		} else {
			return BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
					context.getUsername()), context.getChannel())
					.then();
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start or join a blackjack game.")
				.addArg("bet", false)
				.addField("Info", "**double down** - increase the initial bet by 100% in exchange for committing to stand"
						+ " after receiving exactly one more card", false)
				.build();
	}
}
