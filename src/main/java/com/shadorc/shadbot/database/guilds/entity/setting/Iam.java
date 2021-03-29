package com.shadorc.shadbot.database.guilds.entity.setting;

import com.shadorc.shadbot.database.SerializableEntity;
import com.shadorc.shadbot.database.guilds.bean.setting.IamBean;
import discord4j.common.util.Snowflake;

public class Iam extends SerializableEntity<IamBean> {

    public Iam(IamBean bean) {
        super(bean);
    }

    public Iam(Snowflake messageId, Snowflake roleId) {
        super(new IamBean(messageId.asString(), roleId.asString()));
    }

    public Snowflake getMessageId() {
        return Snowflake.of(this.getBean().getMessageId());
    }

    public Snowflake getRoleId() {
        return Snowflake.of(this.getBean().getRoleId());
    }

}
