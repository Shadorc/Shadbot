package com.shadorc.shadbot.command.game.russianroulette;

import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.util.Snowflake;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RussianRoulettePlayer extends GamblerPlayer {

    private static final int MAX_BULLETS = 6;

    private Instant lastTimePlayed;
    private Instant deadInstant;
    private int bulletIndex;
    private int index;

    public RussianRoulettePlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId, Constants.PAID_COST);
        this.init();
    }

    private void init() {
        this.lastTimePlayed = null;
        this.deadInstant = null;
        this.bulletIndex = ThreadLocalRandom.current().nextInt(1, MAX_BULLETS + 1);
        this.index = 6;
    }

    public void fire() {
        if (this.bulletIndex == this.index) {
            this.deadInstant = Instant.now();
        }
        this.index--;
        this.lastTimePlayed = Instant.now();
    }

    public int getIndex() {
        return this.index;
    }

    public int getRemaining() {
        return MAX_BULLETS - this.index;
    }

    public Duration getResetDuration() {
        if (this.deadInstant == null) {
            return Duration.ZERO;
        }
        return Duration.ofHours(Constants.RESET_HOURS)
                .minus(Duration.ofMillis(TimeUtil.elapsed(this.deadInstant)));
    }

    public boolean isAlive() {
        // If the time is elapsed since the player last played, reset
        if (this.lastTimePlayed != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtil.elapsed(this.lastTimePlayed)) >= Constants.RESET_HOURS) {
            this.init();
        }

        // If the player has been dead for one day, reset
        if (this.deadInstant != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtil.elapsed(this.deadInstant)) >= Constants.RESET_HOURS) {
            this.init();
            return true;
        }

        return this.deadInstant == null;
    }

}
