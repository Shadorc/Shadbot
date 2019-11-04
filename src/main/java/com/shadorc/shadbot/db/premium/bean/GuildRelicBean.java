package com.shadorc.shadbot.db.premium.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GuildRelicBean extends RelicBean {

    @JsonProperty("guildId")
    private long guildId;

    public long getGuildId() {
        return this.guildId;
    }

    @Override
    public String toString() {
        return "GuildRelicBean{" +
                "guildId=" + this.guildId +
                "} " + super.toString();
    }

}
