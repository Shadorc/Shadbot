package me.shadorc.shadbot.command.game.blackjack;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class BlackjackCmd extends GameCmd<BlackjackGame> {

	public BlackjackCmd() {
		super(List.of("blackjack"), "bj");
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final int bet = Utils.requireValidBet(context.getMember(), arg);

		final BlackjackGame blackjackManager = this.getManagers().computeIfAbsent(context.getChannelId(),
				channelId -> {
					final BlackjackGame game = new BlackjackGame(this, context);
					game.start();
					return game;
				});

		if(blackjackManager.addPlayerIfAbsent(new BlackjackPlayer(context.getAuthorId(), bet))) {
			if(blackjackManager.allPlayersStanding()) {
				return blackjackManager.end();
			}
			return blackjackManager.show();
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
						context.getUsername()), channel))
				.then();
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
