package me.shadorc.shadbot.data.stats.core;

import java.io.File;
import java.io.IOException;

import me.shadorc.shadbot.data.stats.StatsManager;

public abstract class Statistic<E extends Enum<E>> {

	private final String fileName;
	private final Class<E> enumClass;

	public Statistic(String fileName, Class<E> enumClass) {
		this.fileName = fileName;
		this.enumClass = enumClass;
	}

	public abstract void save() throws IOException;

	public Class<E> getEnumClass() {
		return this.enumClass;
	}

	public E[] getEnumConstants() {
		return this.enumClass.getEnumConstants();
	}

	public File getFile() {
		return new File(StatsManager.STATS_DIR.getPath(), this.fileName);
	}

}
