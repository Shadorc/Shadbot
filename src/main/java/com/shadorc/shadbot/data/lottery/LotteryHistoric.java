package com.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class LotteryHistoric {

    @JsonProperty("jackpot")
    private final long jackpot;
    @JsonProperty("winners_count")
    private final int winnerCount;
    @JsonProperty("number")
    private final int number;

    public LotteryHistoric(long jackpot, int winnerCount, int number) {
        this.jackpot = jackpot;
        this.winnerCount = winnerCount;
        this.number = number;
    }

    public LotteryHistoric() {
        this(0, 0, 0);
    }

    public long getJackpot() {
        return this.jackpot;
    }

    public int getWinnerCount() {
        return this.winnerCount;
    }

    public int getNumber() {
        return this.number;
    }

}
