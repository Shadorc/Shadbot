package me.shadorc.shadbot.data.stats.core;

import com.fasterxml.jackson.databind.JavaType;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MapStatistic<E extends Enum<E>> extends Statistic<E> {

    private final Map<String, AtomicLong> map;

    public MapStatistic(String fileName, Class<E> enumClass) {
        super(fileName, enumClass);
        this.map = new HashMap<>();

        try {
            if (this.getFile().exists()) {
                final JavaType type = Utils.MAPPER.getTypeFactory().constructMapLikeType(Map.class, String.class, AtomicLong.class);
                this.map.putAll(Utils.MAPPER.readValue(this.getFile(), type));
            }
        } catch (final IOException err) {
            LogUtils.error(err, String.format("An error occurred while initializing statistic: %s", this.getFile()));
        }
    }

    public void log(E key) {
        this.map.computeIfAbsent(key.toString(), ignored -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, AtomicLong> getMap() {
        return Collections.unmodifiableMap(this.map);
    }

    public AtomicLong getValue(String key) {
        return this.map.getOrDefault(key, new AtomicLong(0));
    }

    @Override
    public Object getData() {
        return Collections.unmodifiableMap(this.map);
    }

}
