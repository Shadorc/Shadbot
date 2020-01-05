package com.shadorc.shadbot.api.json.gamestats.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubStats {

    public static final SubStats DEFAULT = new SubStats();

    @JsonProperty("top1")
    private final StatValue top1;
    @JsonProperty("kd")
    private final StatValue ratio;

    public SubStats() {
        this.top1 = new StatValue();
        this.ratio = new StatValue();
    }

    public StatValue getTop1() {
        return this.top1;
    }

    public StatValue getRatio() {
        return this.ratio;
    }

}
