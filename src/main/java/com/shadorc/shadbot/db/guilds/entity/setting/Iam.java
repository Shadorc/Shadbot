package com.shadorc.shadbot.db.guilds.entity.setting;

import com.shadorc.shadbot.db.guilds.bean.setting.IamBean;
import discord4j.core.object.util.Snowflake;

public class Iam {

    private final IamBean bean;

    public Iam(IamBean bean) {
        this.bean = bean;
    }

    public Iam(Snowflake messageId, Snowflake roleId) {
        this.bean = new IamBean(messageId.asString(), roleId.asString());
    }

    public IamBean getBean() {
        return this.bean;
    }

    public Snowflake getMessageId() {
        return Snowflake.of(this.bean.getMessageId());
    }

    public Snowflake getRoleId() {
        return Snowflake.of(this.bean.getRoleId());
    }

}
