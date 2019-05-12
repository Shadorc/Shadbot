package me.shadorc.shadbot.command.game.roulette;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.Player;

public class RoulettePlayer extends Player {

    private final long bet;
    private final String place;

    public RoulettePlayer(Snowflake userId, long bet, String place) {
        super(userId);
        this.bet = bet;
        this.place = place;
    }

    public long getBet() {
        return this.bet;
    }

    public String getPlace() {
        return this.place;
    }

}
