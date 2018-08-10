package me.shadorc.shadbot.data.stats.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JavaType;

import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class MapStat<T extends Enum<T>> extends Stat {

	private Map<String, AtomicLong> map;

	public MapStat(File file) {
		super(file);
		this.map = new HashMap<>();

		try {
			if(file.exists()) {
				final JavaType type = Utils.MAPPER.getTypeFactory().constructMapLikeType(HashMap.class, String.class, AtomicLong.class);
				this.map = Utils.MAPPER.readValue(file, type);
			}
		} catch (IOException err) {
			LogUtils.error(err, String.format("An error occurred while initializing statistic: %s", file));
		}
	}

	public void log(T key) {
		map.computeIfAbsent(key.toString(), ignored -> new AtomicLong(0)).incrementAndGet();
	}

	@Override
	public void save() throws IOException {
		try (FileWriter writer = new FileWriter(this.getFile())) {
			writer.write(Utils.MAPPER.writeValueAsString(map));
		}
	}

}
