package me.shadorc.shadbot.data.stats.core;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.HashBasedTable;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class TableStatistic<E extends Enum<E>> extends Statistic<E> {

	private HashBasedTable<String, String, AtomicLong> table;

	public TableStatistic(String fileName, Class<E> enumClass) {
		super(fileName, enumClass);
		this.table = HashBasedTable.create();

		try {
			if(this.getFile().exists()) {
				final JavaType type = Utils.MAPPER.getTypeFactory().constructMapLikeType(
						HashMap.class,
						Utils.MAPPER.getTypeFactory().constructType(String.class),
						Utils.MAPPER.getTypeFactory().constructParametricType(HashMap.class, String.class, AtomicLong.class));
				this.table = Utils.toTable(Utils.MAPPER.readValue(this.getFile(), type));
			}
		} catch (IOException err) {
			LogUtils.error(err, String.format("An error occurred while initializing statistic: %s", this.getFile()));
		}
	}

	public void log(E rowKey, String columnKey, long value) {
		synchronized (table) {
			if(!table.contains(rowKey.toString(), columnKey)) {
				table.put(rowKey.toString(), columnKey, new AtomicLong(0));
			}
			table.get(rowKey.toString(), columnKey).addAndGet(value);
		}
	}

	public void log(E rowKey, AbstractCommand cmd) {
		this.log(rowKey, cmd.getName(), 1);
	}

	public Map<String, AtomicLong> getMap(String rowKey) {
		synchronized (table) {
			return table.row(rowKey);
		}
	}

	public Map<String, AtomicLong> getMap(E rowKey) {
		return this.getMap(rowKey.toString());
	}

	@Override
	public void save() throws IOException {
		synchronized (table) {
			try (FileWriter writer = new FileWriter(this.getFile())) {
				writer.write(Utils.MAPPER.writeValueAsString(table.rowMap()));
			}
		}
	}

}
