package me.shadorc.shadbot.data.lotto;

public class LottoHistoric {

	private final int pool;
	private final int winnersCount;
	private final int num;

	public LottoHistoric(int pool, int winnersCount, int num) {
		this.pool = pool;
		this.winnersCount = winnersCount;
		this.num = num;
	}

	public int getPool() {
		return pool;
	}

	public int getWinnersCount() {
		return winnersCount;
	}

	public int getNum() {
		return num;
	}

}
