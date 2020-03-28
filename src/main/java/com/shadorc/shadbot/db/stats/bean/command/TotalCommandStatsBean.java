package com.shadorc.shadbot.db.stats.bean.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

import java.util.Collections;
import java.util.Map;

public class TotalCommandStatsBean implements Bean {

    // Map<Day, Map<CmdName, Usages>>
    @JsonProperty("command_stats")
    private Map<Integer, Map<String, Integer>> commandStats;

    public Map<Integer, Map<String, Integer>> getCommandStats() {
        return Collections.unmodifiableMap(this.commandStats);
    }

    @Override
    public String toString() {
        return "TotalCommandStatsBean{" +
                "commandStats=" + this.commandStats +
                '}';
    }
}
