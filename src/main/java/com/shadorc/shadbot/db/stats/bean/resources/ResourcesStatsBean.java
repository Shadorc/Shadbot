package com.shadorc.shadbot.db.stats.bean.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

public class ResourcesStatsBean implements Bean {

    @JsonProperty("cpu_usage")
    private double cpuUsage;
    @JsonProperty("ram_usage")
    private int ramUsage;
    @JsonProperty("timestamp")
    private long timestamp;

    public double getCpuUsage() {
        return this.cpuUsage;
    }

    public int getRamUsage() {
        return this.ramUsage;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return "SystemResourcesStatsBean{" +
                "cpuUsage=" + this.cpuUsage +
                ", ramUsage=" + this.ramUsage +
                ", timestamp=" + this.timestamp +
                '}';
    }
}
