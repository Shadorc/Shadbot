package me.shadorc.shadbot.data.lotto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LottoHistoric {

	@JsonProperty("jackpot")
	private final int jackpot;
	@JsonProperty("winners_count")
	private final int winnersCount;
	@JsonProperty("number")
	private final int number;

	public LottoHistoric(int jackpot, int winnersCount, int number) {
		this.jackpot = jackpot;
		this.winnersCount = winnersCount;
		this.number = number;
	}

	public LottoHistoric() {
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
		return String.format("LottoHistoric [jackpot=%s, winnersCount=%s, number=%s]", this.jackpot, this.winnersCount, this.number);
	}

}
