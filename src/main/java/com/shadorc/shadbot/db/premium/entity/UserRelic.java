package com.shadorc.shadbot.db.premium.entity;

import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.bean.UserRelicBean;
import discord4j.core.object.util.Snowflake;

public class UserRelic extends Relic {

    private final long userId;

    public UserRelic(UserRelicBean bean) {
        super(bean);
        this.userId = bean.getUserId();
    }

    public UserRelic(Snowflake userId, String id, long duration) {
        super(id, RelicType.USER.toString(), duration);
        this.userId = userId.asLong();
    }

    public Snowflake getUserId() {
        return Snowflake.of(this.userId);
    }

}
