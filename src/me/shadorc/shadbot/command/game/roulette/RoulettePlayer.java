package me.shadorc.shadbot.command.game.roulette;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.player.GamblerPlayer;

public class RoulettePlayer extends GamblerPlayer {

    private final String place;

    public RoulettePlayer(Snowflake guildId, Snowflake userId, long bet, String place) {
        super(guildId, userId, bet);
        this.place = place;
    }

    public String getPlace() {
        return this.place;
    }

}
