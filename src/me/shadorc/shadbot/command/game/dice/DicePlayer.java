package me.shadorc.shadbot.command.game.dice;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.player.GamblerPlayer;

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
