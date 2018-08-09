package me.shadorc.shadbot.data.stats.core;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashBasedTable;

public class TableStat <R extends Enum<R>> {

	private final HashBasedTable<R, String, AtomicLong> table;

	public TableStat() {
		this.table = HashBasedTable.create();
	}

	public void log(R rowKey, String columnKey, long value) {
		synchronized (table) {
			if(!table.contains(rowKey, columnKey)) {
				table.put(rowKey, columnKey, new AtomicLong(0));
			}
			table.get(rowKey, columnKey).addAndGet(value);
		}
	}
	
	public void log(R rowKey, String columnKey) {
		this.log(rowKey, columnKey, 1);
	}

	public Map<String, AtomicLong> get(R rowKey) {
		synchronized (table) {
			return table.row(rowKey);
		}
	}

}
