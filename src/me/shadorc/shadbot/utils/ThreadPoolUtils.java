package me.shadorc.shadbot.utils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadPoolUtils {

	public static ThreadFactory getThreadFactoryNamed(String name) {
		return new ThreadFactoryBuilder().setNameFormat(name).build();
	}

	public static ScheduledThreadPoolExecutor newSingleScheduledThreadPoolExecutor(String name) {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, ThreadPoolUtils.getThreadFactoryNamed(name));
		executor.setRemoveOnCancelPolicy(true);
		return executor;
	}

}
