package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.core.game.player.Player;
import discord4j.common.util.Snowflake;

import java.util.concurrent.atomic.AtomicInteger;

public class RpsPlayer extends Player {

    private final AtomicInteger winStreak;

    public RpsPlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId);
        this.winStreak = new AtomicInteger();
    }

    public AtomicInteger getWinStreak() {
        return this.winStreak;
    }

}
