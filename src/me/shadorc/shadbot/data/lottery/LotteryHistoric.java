package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LotteryHistoric {

	@JsonProperty("jackpot")
	private final int jackpot;
	@JsonProperty("winners_count")
	private final int winnerCount;
	@JsonProperty("number")
	private final int number;

	public LotteryHistoric(int jackpot, int winnerCount, int number) {
		this.jackpot = jackpot;
		this.winnerCount = winnerCount;
		this.number = number;
	}

	public LotteryHistoric() {
		this(0, 0, 0);
	}

	public int getJackpot() {
		return this.jackpot;
	}

	public int getWinnerCount() {
		return this.winnerCount;
	}

	public int getNumber() {
		return this.number;
	}

}
