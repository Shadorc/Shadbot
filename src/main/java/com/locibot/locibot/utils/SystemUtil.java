package com.locibot.locibot.utils;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.*;
import java.time.Duration;
import java.util.List;

public class SystemUtil {

    private static final int MB_UNIT = 1024 << 10;
    private static final OperatingSystemMXBean OS_BEAN = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean RUNTIME_BEAN = ManagementFactory.getRuntimeMXBean();
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
    private static final MemoryUsage HEAP_MEMORY_BEAN = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    private static final List<GarbageCollectorMXBean> GC_BEAN = ManagementFactory.getGarbageCollectorMXBeans();
    private static final Runtime RUNTIME = Runtime.getRuntime();

    /**
     * @return The uptime of the JVM in milliseconds.
     */
    public static Duration getUptime() {
        return Duration.ofMillis(RUNTIME_BEAN.getUptime());
    }

    /**
     * @return The recent CPU usage for the JVM process in percentage.
     */
    public static double getProcessCpuUsage() {
        return OS_BEAN.getProcessCpuLoad() * 100.0d;
    }

    /**
     * @return The recent CPU usage for the operating environment in percentage.
     */
    public static double getSystemCpuUsage() {
        return OS_BEAN.getCpuLoad() * 100.0d;
    }

    /**
     * @return The maximum amount of memory in megabytes.
     */
    public static long getMaxHeapMemory() {
        return HEAP_MEMORY_BEAN.getMax() / MB_UNIT;
    }

    /**
     * @return The amount of committed memory in megabytes.
     */
    public static long getTotalHeapMemory() {
        return HEAP_MEMORY_BEAN.getCommitted() / MB_UNIT;
    }

    /**
     * @return The amount of used memory in megabytes.
     */
    public static long getUsedHeapMemory() {
        return HEAP_MEMORY_BEAN.getUsed() / MB_UNIT;
    }

    /**
     * @return The total amount of memory available on the system in megabytes.
     */
    public static long getSystemTotalMemory() {
        return OS_BEAN.getTotalMemorySize() / MB_UNIT;
    }

    /**
     * @return The amount of free memory available on the system in megabytes.
     */
    public static long getSystemFreeMemory() {
        return OS_BEAN.getFreeMemorySize() / MB_UNIT;
    }

    /**
     * @return The total amount of memory in the Java virtual machine in megabytes.
     */
    public static long getProcessTotalMemory() {
        return RUNTIME.totalMemory() / MB_UNIT;
    }

    /**
     * @return The amount of free memory in the Java Virtual Machine in megabytes.
     */
    public static long getProcessFreeMemory() {
        return RUNTIME.freeMemory() / MB_UNIT;
    }

    /**
     * @return Returns the maximum amount of memory that the Java virtual machine will attempt to use in megabytes.
     */
    public static long getProcessMaxMemory() {
        return RUNTIME.maxMemory() / MB_UNIT;
    }

    /**
     * @return The total number of Garbage Collection count.
     */
    public static long getGCCount() {
        return GC_BEAN.stream()
                .map(GarbageCollectorMXBean::getCollectionCount)
                .mapToInt(Long::intValue)
                .filter(count -> count > 0)
                .sum();
    }

    /**
     * @return The total Garbage Collection Time.
     */
    public static Duration getGCTime() {
        return Duration.ofMillis(GC_BEAN.stream()
                .map(GarbageCollectorMXBean::getCollectionTime)
                .mapToInt(Long::intValue)
                .filter(time -> time > 0)
                .sum());
    }

    /**
     * @return The current number of live threads.
     */
    public static int getThreadCount() {
        return THREAD_BEAN.getThreadCount();
    }

    /**
     * @return The current number of live daemon threads.
     */
    public static int getDaemonThreadCount() {
        return THREAD_BEAN.getDaemonThreadCount();
    }

}
