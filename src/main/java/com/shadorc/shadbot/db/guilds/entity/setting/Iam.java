package com.shadorc.shadbot.db.guilds.entity.setting;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.setting.IamBean;
import discord4j.rest.util.Snowflake;

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
