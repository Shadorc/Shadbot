package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import discord4j.common.util.Snowflake;

public class DicePlayer extends GamblerPlayer {

    private final int number;

    public DicePlayer(Snowflake guildId, Snowflake userId, long bet, int number) {
        super(guildId, userId, bet);
        this.number = number;
    }

    public int getNumber() {
        return this.number;
    }

}
