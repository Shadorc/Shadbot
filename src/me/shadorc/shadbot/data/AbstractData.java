package me.shadorc.shadbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import me.shadorc.discordbot.data.Config;
import me.shadorc.shadbot.Scheduler;
import me.shadorc.shadbot.utils.LogUtils;

public abstract class AbstractData {

	private final File file;
	private final int initialDelay;
	private final int period;
	private final TimeUnit unit;

	public AbstractData(File file, int initialDelay, int period, TimeUnit unit) {
		this.file = file;
		this.initialDelay = initialDelay;
		this.period = period;
		this.unit = unit;

		Scheduler.register(this);
	}

	public abstract JSONObject getJSON();

	public final int getInitialDelay() {
		return initialDelay;
	}

	public final int getPeriod() {
		return period;
	}

	public final TimeUnit getUnit() {
		return unit;
	}

	public final void save() {
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(this.getJSON().toString(Config.INDENT_FACTOR));

		} catch (IOException err) {
			LogUtils.error("Error while saving database.", err);
		}
		//		LogUtils.info("Database saved.");
	}

}
