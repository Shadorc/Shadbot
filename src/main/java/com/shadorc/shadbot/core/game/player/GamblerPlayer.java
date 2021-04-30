package com.shadorc.shadbot.core.game.player;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

public class GamblerPlayer extends Player {

    private final long bet;

    public GamblerPlayer(Snowflake guildId, Snowflake userId, String username, long bet) {
        super(guildId, userId, username);
        this.bet = bet;
    }

    public GamblerPlayer(Snowflake guildId, Snowflake userId, long bet) {
        this(guildId, userId, null, bet);
    }

    public long getBet() {
        return this.bet;
    }

    public Mono<Void> bet() {
        return this.lose(this.bet);
    }

    // TODO: Does any other game than Blackjack need this?
    public Mono<Void> cancelBet() {
        return this.win(this.bet);
    }

}
