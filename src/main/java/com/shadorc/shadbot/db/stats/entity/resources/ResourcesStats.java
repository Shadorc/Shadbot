package com.shadorc.shadbot.db.stats.entity.resources;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.stats.bean.resources.ResourcesStatsBean;

import java.time.Instant;

public class ResourcesStats extends SerializableEntity<ResourcesStatsBean> {

    public ResourcesStats(ResourcesStatsBean bean) {
        super(bean);
    }

    public double getCpuUsage() {
        return this.getBean().getCpuUsage();
    }

    public int getRamUsage() {
        return this.getBean().getRamUsage();
    }

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(this.getBean().getTimestamp());
    }

    @Override
    public String toString() {
        return "ResourcesStats{" +
                "bean=" + this.getBean() +
                '}';
    }

}
