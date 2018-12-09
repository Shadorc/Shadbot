package me.shadorc.shadbot.data.lottery;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LotteryHistoric {

	@JsonProperty("jackpot")
	private final int jackpot;
	@JsonProperty("winners_count")
	private final int winnersCount;
	@JsonProperty("number")
	private final int number;

	public LotteryHistoric(int jackpot, int winnersCount, int number) {
		this.jackpot = jackpot;
		this.winnersCount = winnersCount;
		this.number = number;
	}

	public LotteryHistoric() {
		this(0, 0, 0);
	}

	public int getJackpot() {
		return this.jackpot;
	}

	public int getWinnersCount() {
		return this.winnersCount;
	}

	public int getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		return String.format("LotteryHistoric [jackpot=%s, winnersCount=%s, number=%s]", jackpot, winnersCount, number);
	}

}
