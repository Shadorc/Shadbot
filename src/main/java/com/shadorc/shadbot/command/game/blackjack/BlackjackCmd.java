package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class BlackjackCmd extends GameCmd<BlackjackGame> {

    public BlackjackCmd() {
        super("blackjack", "Start or join a blackjack game");

        this.addOption(option -> option.name("bet")
                .description("Your bet")
                .type(ApplicationCommandOptionType.INTEGER.getValue())
                .required(true));
    }

    @Override
    public Mono<?> execute(Context context) {
        final long bet = context.getOptionAsLong("bet").orElseThrow();

        return ShadbotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), bet)
                .flatMap(__ -> {
                    if (this.isGameStarted(context.getChannelId())) {
                        return Mono.just(this.getGame(context.getChannelId()));
                    } else {
                        final BlackjackGame game = new BlackjackGame(context);
                        this.addGame(context.getChannelId(), game);
                        return game.start()
                                .doOnError(err -> game.destroy())
                                .thenReturn(game);
                    }
                })
                .flatMap(blackjackGame -> {
                    final BlackjackPlayer player = new BlackjackPlayer(context.getGuildId(), context.getAuthorId(),
                            context.getAuthorName(), bet);
                    if (blackjackGame.addPlayerIfAbsent(player)) {
                        return player.bet()
                                .then(context.reply(Emoji.CHECK_MARK, context.localize("blackjack.joined")))
                                .then(Mono.defer(() -> {
                                    if (blackjackGame.areAllPlayersStanding()) {
                                        return blackjackGame.end();
                                    } else {
                                        return blackjackGame.show();
                                    }
                                }));
                    }

                    return context.reply(Emoji.INFO, context.localize("blackjack.already.participating"));
                });
    }

}
