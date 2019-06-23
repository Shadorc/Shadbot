package com.shadorc.shadbot.core.game.player;

import discord4j.core.object.util.Snowflake;

public class GamblerPlayer extends Player {

    private final long bet;

    public GamblerPlayer(Snowflake guildId, Snowflake userId, long bet) {
        super(guildId, userId);
        this.bet = bet;
    }

    public long getBet() {
        return this.bet;
    }

    public void bet() {
        this.lose(this.bet);
    }

    public void cancelBet() {
        this.win(this.bet);
    }

    public void draw() {
        this.cancelBet();
    }

}
