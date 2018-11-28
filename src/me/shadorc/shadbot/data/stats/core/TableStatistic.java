package me.shadorc.shadbot.data.stats.core;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.HashBasedTable;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class TableStatistic<E extends Enum<E>> extends Statistic<E> {

	private final HashBasedTable<String, String, AtomicLong> table;

	public TableStatistic(String fileName, Class<E> enumClass) {
		super(fileName, enumClass);
		this.table = HashBasedTable.create();

		try {
			if(this.getFile().exists()) {
				final JavaType type = Utils.MAPPER.getTypeFactory().constructMapLikeType(
						Map.class,
						Utils.MAPPER.getTypeFactory().constructType(String.class),
						Utils.MAPPER.getTypeFactory().constructParametricType(Map.class, String.class, AtomicLong.class));
				this.table.putAll(Utils.toTable(Utils.MAPPER.readValue(this.getFile(), type)));
			}
		} catch (IOException err) {
			LogUtils.error(err, String.format("An error occurred while initializing statistic: %s", this.getFile()));
		}
	}

	public void log(E rowKey, String columnKey, long value) {
		synchronized (this.table) {
			if(!this.table.contains(rowKey.toString(), columnKey)) {
				this.table.put(rowKey.toString(), columnKey, new AtomicLong(0));
			}
			this.table.get(rowKey.toString(), columnKey).addAndGet(value);
		}
	}

	public void log(E rowKey, AbstractCommand cmd) {
		this.log(rowKey, cmd.getName(), 1);
	}

	public Map<String, AtomicLong> getMap(String rowKey) {
		synchronized (this.table) {
			return this.table.row(rowKey);
		}
	}

	public Map<String, AtomicLong> getMap(E rowKey) {
		return this.getMap(rowKey.toString());
	}

	@Override
	public Object getData() {
		synchronized (this.table) {
			return this.table.rowMap();
		}
	}

}
