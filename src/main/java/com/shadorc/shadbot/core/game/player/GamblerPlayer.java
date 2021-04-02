package com.shadorc.shadbot.core.game.player;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

public class GamblerPlayer extends Player {

    private final long bet;

    public GamblerPlayer(Snowflake guildId, Snowflake userId, long bet) {
        super(guildId, userId);
        this.bet = bet;
    }

    public long getBet() {
        return this.bet;
    }

    public Mono<Void> bet() {
        return this.lose(this.bet);
    }

    // TODO: Bet only when needed?
    public Mono<Void> cancelBet() {
        return this.win(this.bet);
    }

}
