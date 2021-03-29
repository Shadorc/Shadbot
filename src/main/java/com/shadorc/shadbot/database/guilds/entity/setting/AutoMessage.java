package com.shadorc.shadbot.database.guilds.entity.setting;

import com.shadorc.shadbot.database.SerializableEntity;
import com.shadorc.shadbot.database.guilds.bean.setting.AutoMessageBean;
import discord4j.common.util.Snowflake;

public class AutoMessage extends SerializableEntity<AutoMessageBean> {

    public AutoMessage(AutoMessageBean bean) {
        super(bean);
    }

    public AutoMessage(String message, Snowflake channelId) {
        super(new AutoMessageBean(message, channelId.asString()));
    }

    public String getMessage() {
        return this.getBean().getMessage();
    }

    public Snowflake getChannelId() {
        return Snowflake.of(this.getBean().getChannelId());
    }

}
