package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.function.Consumer;

public class BlackjackCmd extends GameCmd<BlackjackGame> {

    public BlackjackCmd() {
        super(List.of("blackjack"), "bj");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return Utils.requireValidBet(context.getGuildId(), context.getAuthorId(), arg)
                .flatMap(bet -> {
                    if (this.getManagers().containsKey(context.getChannelId())) {
                        return Mono.just(Tuples.of(this.getManagers().get(context.getChannelId()), bet));
                    } else {
                        final BlackjackGame game = new BlackjackGame(this, context);
                        this.getManagers().put(context.getChannelId(), game);
                        return game.start()
                                .thenReturn(game)
                                .zipWith(Mono.just(bet));
                    }
                })
                .flatMap(tuple -> {
                    final BlackjackGame blackjackGame = tuple.getT1();
                    final long bet = tuple.getT2();

                    final BlackjackPlayer player = new BlackjackPlayer(context.getGuildId(), context.getAuthorId(), bet);
                    // If the user was successfully added as a player
                    if (blackjackGame.addPlayerIfAbsent(player)) {
                        return player.bet()
                                .then(Mono.defer(() -> {
                                    if (blackjackGame.areAllPlayersStanding()) {
                                        return blackjackGame.end();
                                    } else {
                                        return blackjackGame.show();
                                    }
                                }));
                    }

                    return context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.INFO + " (**%s**) You're already participating.",
                                            context.getUsername()), channel))
                            .then();
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Start or join a blackjack game.")
                .addArg("bet", false)
                .addField("Info", "**double down** - increase the initial bet by 100% in exchange for "
                        + "committing to stand after receiving exactly one more card", false)
                .addField("Rules", "This game follows the same rules as real Blackjack.", false)
                .addField("Gains", String.format("Gains are multiplied by **%.1f** if you win.", Constants.WIN_MULTIPLICATOR), false)
                .build();
    }
}
