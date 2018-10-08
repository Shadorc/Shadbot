package me.shadorc.shadbot.data.lotto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LottoHistoric {

	@JsonProperty("jackpot")
	private int jackpot;
	@JsonProperty("winners_count")
	private int winnersCount;
	@JsonProperty("number")
	private int number;

	public LottoHistoric() {
		this.jackpot = 0;
		this.winnersCount = 0;
		this.number = 0;
	}

	public LottoHistoric(int jackpot, int winnersCount, int number) {
		this.jackpot = jackpot;
		this.winnersCount = winnersCount;
		this.number = number;
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
		return String.format("LottoHistoric [jackpot=%s, winnersCount=%s, number=%s]", this.jackpot, this.winnersCount, this.number);
	}

}
