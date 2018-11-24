package me.shadorc.shadbot.data.lotto;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;

import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.utils.Utils;

public class LottoManager extends Data {

	private Lotto lotto;

	public LottoManager() throws IOException {
		super("lotto_data.json", Duration.ofMinutes(30), Duration.ofMinutes(30));

		this.lotto = this.getFile().exists() ? Utils.MAPPER.readValue(this.getFile(), Lotto.class) : new Lotto();
	}

	public void write() throws IOException {
		try (FileWriter writer = new FileWriter(this.getFile())) {
			writer.write(Utils.MAPPER.writeValueAsString(lotto));
		}
	}

	public Lotto getLotto() {
		return this.lotto;
	}

}
