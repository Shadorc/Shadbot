package com.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.util.Snowflake;
import reactor.util.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Lottery {

    @JsonProperty("historic")
    private LotteryHistoric historic;
    @JsonProperty("jackpot")
    private final AtomicLong jackpot;
    @JsonProperty("gamblers")
    private final List<LotteryGambler> gamblers;

    public Lottery() {
        this.historic = null;
        this.jackpot = new AtomicLong(0);
        this.gamblers = new CopyOnWriteArrayList<>();
    }

    @Nullable
    public LotteryHistoric getHistoric() {
        return this.historic;
    }

    public long getJackpot() {
        return this.jackpot.get();
    }

    public List<LotteryGambler> getGamblers() {
        return Collections.unmodifiableList(this.gamblers);
    }

    public void setHistoric(LotteryHistoric historic) {
        this.historic = historic;
    }

    public void addToJackpot(long coins) {
        final long newPool = this.jackpot.get() + (int) Math.ceil(coins / 100.0f);
        this.jackpot.set((int) NumberUtils.truncateBetween(newPool, 0, Config.MAX_COINS));
    }

    public void addGambler(Snowflake guildId, Snowflake userId, int number) {
        this.gamblers.add(new LotteryGambler(guildId, userId, number));
    }

    public void resetJackpot() {
        this.jackpot.set(0);
    }

    public void resetGamblers() {
        this.gamblers.clear();
    }

}
