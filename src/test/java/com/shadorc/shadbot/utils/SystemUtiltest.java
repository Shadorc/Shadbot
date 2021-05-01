package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemUtiltest {

    @Test
    public void testGetUptime() {
        assertNotNull(SystemUtil.getUptime());
    }

    @Test
    public void testGetProcessCpuUsage() {
        assertTrue(SystemUtil.getProcessCpuUsage() > 0);
    }

    @Test
    public void testGetSystemCpuUsage() {
        assertTrue(SystemUtil.getSystemCpuUsage() > 0);
    }

    @Test
    public void testGetMaxHeapMemory() {
        assertTrue(SystemUtil.getMaxHeapMemory() > 0);
    }

    @Test
    public void testGetTotalHeapMemory() {
        assertTrue(SystemUtil.getTotalHeapMemory() > 0);
    }

    @Test
    public void testGetUsedHeapMemory() {
        assertTrue(SystemUtil.getUsedHeapMemory() > 0);
    }

    @Test
    public void testGetTotalMemory() {
        assertTrue(SystemUtil.getTotalMemory() > 0);
    }

    @Test
    public void testGetFreeMemory() {
        assertTrue(SystemUtil.getFreeMemory() > 0);
    }

    @Test
    public void testGetGCCount() {
        assertTrue(SystemUtil.getGCCount() > 0);
    }

    @Test
    public void testGetGCTime() {
        assertTrue(SystemUtil.getGCTime().toMillis() > 0);
    }

    @Test
    public void testGetThreadCount() {
        assertTrue(SystemUtil.getThreadCount() > 0);
    }

    @Test
    public void testGetDaemonThreadCount() {
        assertTrue(SystemUtil.getDaemonThreadCount() > 0);
    }

}
