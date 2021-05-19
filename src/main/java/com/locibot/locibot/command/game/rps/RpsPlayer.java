package com.locibot.locibot.command.game.rps;

import com.locibot.locibot.core.game.player.Player;
import discord4j.common.util.Snowflake;

import java.util.concurrent.atomic.AtomicInteger;

public class RpsPlayer extends Player {

    private final AtomicInteger winStreak;

    public RpsPlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId);
        this.winStreak = new AtomicInteger();
    }

    public int getWinStreak() {
        return this.winStreak.get();
    }

    public void incrementWinStream() {
        this.winStreak.incrementAndGet();
    }

    public void resetWinStreak() {
        this.winStreak.set(0);
    }

}
