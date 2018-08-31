package me.shadorc.shadbot.data.stats.core;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JavaType;

import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class MapStatistic<E extends Enum<E>> extends Statistic<E> {

	private Map<String, AtomicLong> map;

	public MapStatistic(String fileName, Class<E> enumClass) {
		super(fileName, enumClass);
		this.map = new HashMap<>();

		try {
			if(this.getFile().exists()) {
				final JavaType type = Utils.MAPPER.getTypeFactory().constructMapLikeType(HashMap.class, String.class, AtomicLong.class);
				this.map = Utils.MAPPER.readValue(this.getFile(), type);
			}
		} catch (IOException err) {
			LogUtils.error(err, String.format("An error occurred while initializing statistic: %s", this.getFile()));
		}
	}

	public void log(E key) {
		this.map.computeIfAbsent(key.toString(), ignored -> new AtomicLong(0)).incrementAndGet();
	}

	public Map<String, AtomicLong> getMap() {
		return this.map;
	}

	public AtomicLong getValue(String key) {
		return this.map.getOrDefault(key, new AtomicLong(0));
	}

	@Override
	public void save() throws IOException {
		try (FileWriter writer = new FileWriter(this.getFile())) {
			writer.write(Utils.MAPPER.writeValueAsString(this.map));
		}
	}

}
