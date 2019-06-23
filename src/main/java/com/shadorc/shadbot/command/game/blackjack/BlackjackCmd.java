package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
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

        final long bet = Utils.requireValidBet(context.getMember(), arg);

        final BlackjackGame blackjackManager = this.getManagers().computeIfAbsent(context.getChannelId(),
                channelId -> {
                    final BlackjackGame game = new BlackjackGame(this, context);
                    game.start();
                    return game;
                });

        final BlackjackPlayer player = new BlackjackPlayer(context.getGuildId(), context.getAuthorId(), bet);
        if (blackjackManager.addPlayerIfAbsent(player)) {
            player.bet();
            if (blackjackManager.areAllPlayersStanding()) {
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
                .addField("Gains", "This game follows the same rules and winnings as real Blackjack.", false)
                .build();
    }
}
