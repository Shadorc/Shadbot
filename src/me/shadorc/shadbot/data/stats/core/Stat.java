package me.shadorc.shadbot.data.stats.core;

import java.io.File;
import java.io.IOException;

import me.shadorc.shadbot.data.stats.StatsManager;

public abstract class Stat {

	private final File file;

	public Stat(File file) {
		this.file = file;
	}

	public abstract void save() throws IOException;

	public File getFile() {
		return new File(StatsManager.STATS_DIR.getPath(), file.getName());
	}

}
