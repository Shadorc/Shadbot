package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;
import reactor.util.annotation.Nullable;

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
        return this.gamblers;
    }

    public void setHistoric(LotteryHistoric historic) {
        this.historic = historic;
    }

    public void addToJackpot(long coins) {
        final long newPool = this.jackpot.get() + (int) Math.ceil(coins / 100f);
        this.jackpot.set(NumberUtils.between(newPool, 0, Config.MAX_COINS));
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
