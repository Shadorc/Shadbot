/*
package com.shadorc.shadbot.command.game.russianroulette;

import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.common.util.Snowflake;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RussianRoulettePlayer extends GamblerPlayer {

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
        this.bulletIndex = ThreadLocalRandom.current().nextInt(1, 7);
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
        return 6 - this.index;
    }

    public boolean isAlive() {
        // If the player has not played since one day, reset
        if (this.lastTimePlayed != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtils.getMillisUntil(this.lastTimePlayed)) >= Constants.RESET_HOURS) {
            this.init();
        }

        // If the player has been dead for one day, reset
        if (this.deadInstant != null
                && TimeUnit.MILLISECONDS.toHours(TimeUtils.getMillisUntil(this.deadInstant)) >= Constants.RESET_HOURS) {
            this.init();
            return true;
        }

        return this.deadInstant == null;
    }

}
*/
