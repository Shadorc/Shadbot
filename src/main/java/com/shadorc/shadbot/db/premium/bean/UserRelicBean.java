package com.shadorc.shadbot.db.premium.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserRelicBean extends RelicBean {

    @JsonProperty("userId")
    private long userId;

    public long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "UserRelic{" +
                "userId=" + userId +
                "} " + super.toString();
    }
}
