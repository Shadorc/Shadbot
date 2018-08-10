package me.shadorc.shadbot.data.lotto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.Utils;

public class LottoManager {

	private static final File FILE = new File(DataManager.SAVE_DIR, "lotto_data.json");

	private static Lotto lotto;

	@DataInit
	public static void init() throws IOException {
		lotto = FILE.exists() ? Utils.MAPPER.readValue(FILE, Lotto.class) : new Lotto();
	}

	@DataSave(initialDelay = 30, period = 30, unit = ChronoUnit.MINUTES)
	public static void save() throws IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(Utils.MAPPER.writeValueAsString(lotto));
		}
	}

	public static Lotto getLotto() {
		return lotto;
	}

}
