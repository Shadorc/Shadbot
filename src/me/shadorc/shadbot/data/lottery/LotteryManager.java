package me.shadorc.shadbot.data.lottery;

import java.io.IOException;
import java.time.Duration;

import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.utils.Utils;

public class LotteryManager extends Data {

	private final Lottery lottery;

	public LotteryManager() throws IOException {
		super("lotto_data.json", Duration.ofMinutes(30), Duration.ofMinutes(30));

		this.lottery = this.getFile().exists() ? Utils.MAPPER.readValue(this.getFile(), Lottery.class) : new Lottery();
	}

	public Lottery getLottery() {
		return this.lottery;
	}

	@Override
	public Object getData() {
		return this.getLottery();
	}

}
