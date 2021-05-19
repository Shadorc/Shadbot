package com.locibot.locibot.database.guilds.bean.setting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;

public class AutoMessageBean implements Bean {

    @JsonProperty("message")
    private String message;
    @JsonProperty("channel_id")
    private String channelId;

    public AutoMessageBean(String message, String channelId) {
        this.message = message;
        this.channelId = channelId;
    }

    public AutoMessageBean() {

    }

    public String getMessage() {
        return this.message;
    }

    public String getChannelId() {
        return this.channelId;
    }

    @Override
    public String toString() {
        return "AutoMessageBean{" +
                "message='" + this.message + '\'' +
                ", channelId='" + this.channelId + '\'' +
                '}';
    }
}
