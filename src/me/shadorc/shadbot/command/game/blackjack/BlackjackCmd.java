package me.shadorc.shadbot.command.game.blackjack;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class BlackjackCmd extends GameCmd<BlackjackManager> {

	private static final int MAX_BET = 250_000;

	public BlackjackCmd() {
		super(List.of("blackjack"), "bj");
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Integer bet = Utils.requireBet(context.getMember(), arg, MAX_BET);

		final BlackjackManager blackjackManager = this.getManagers().computeIfAbsent(context.getChannelId(),
				channelId -> {
					final BlackjackManager manager = new BlackjackManager(this, context);
					manager.start();
					return manager;
				});

		if(blackjackManager.addPlayerIfAbsent(context.getAuthorId(), context.getUsername(), bet)) {
			if(blackjackManager.allPlayersStanding()) {
				return blackjackManager.end();
			}
			return blackjackManager.show();
		} else {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
							context.getUsername()), channel))
					.then();
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start or join a blackjack game.")
				.addArg("bet", false)
				.addField("Info", "**double down** - increase the initial bet by 100% in exchange for committing to stand"
						+ " after receiving exactly one more card", false)
				.build();
	}
}
