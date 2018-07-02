package me.shadorc.shadbot.data.lotto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.Utils;

public class LottoManager {

	private static final String FILE_NAME = "lotto_data.json";
	private static final File FILE = new File(DataManager.SAVE_DIR, FILE_NAME);

	private static Lotto lotto;

	@DataInit
	public static void init() throws JsonParseException, JsonMappingException, IOException {
		lotto = Utils.MAPPER.readValue(FILE, Lotto.class);
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 30, period = 30, unit = TimeUnit.MINUTES)
	public static void save() throws JsonProcessingException, IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(Utils.MAPPER.writeValueAsString(lotto));
		}
	}

	public static Lotto getLotto() {
		return lotto;
	}

}
