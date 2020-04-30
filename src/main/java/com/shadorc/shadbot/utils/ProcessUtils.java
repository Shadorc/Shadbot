package com.shadorc.shadbot.utils;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class ProcessUtils {

    private static final int MB_UNIT = 1024 << 10;

    /**
     * @return CPU utilisation in percentage.
     */
    public static double getCpuUsage() {
        return ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getProcessCpuLoad() * 100.0d;
    }

    /**
     * @return The amount of memory used in Mb.
     */
    public static long getMemoryUsed() {
        final Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / MB_UNIT;
    }

    /**
     * @return The maximum amount of memory allocated in Mb.
     */
    public static long getMaxMemory() {
        final Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() / MB_UNIT;
    }

    /**
     * @return The total number of Garbage Collection count.
     */
    public static long getGCCount() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(GarbageCollectorMXBean::getCollectionCount)
                .mapToInt(Long::intValue)
                .filter(count -> count > 0)
                .sum();
    }

    /**
     * @return The total Garbage Collection Time (ms)
     */
    public static long getGCTime() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(GarbageCollectorMXBean::getCollectionTime)
                .mapToInt(Long::intValue)
                .filter(time -> time > 0)
                .sum();
    }

}
