package com.locibot.locibot.command.game.roulette;

import com.locibot.locibot.core.game.player.GamblerPlayer;
import discord4j.common.util.Snowflake;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class RoulettePlayer extends GamblerPlayer {

    private final RouletteCmd.Place place;
    @Nullable
    private final Long number;

    public RoulettePlayer(Snowflake guildId, Snowflake userId, String username, long bet, RouletteCmd.Place place,
                          @Nullable Long number) {
        super(guildId, userId, username, bet);
        this.place = place;
        this.number = number;
    }

    public RouletteCmd.Place getPlace() {
        return this.place;
    }

    public Optional<Long> getNumber() {
        return Optional.ofNullable(this.number);
    }

}
