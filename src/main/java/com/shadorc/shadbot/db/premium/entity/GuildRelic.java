package com.shadorc.shadbot.db.premium.entity;

import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.bean.GuildRelicBean;
import discord4j.core.object.util.Snowflake;

public class GuildRelic extends Relic {

    private final long guildId;

    public GuildRelic(GuildRelicBean bean) {
        super(bean);
        this.guildId = bean.getGuildId();
    }

    public GuildRelic(Snowflake guildId, String id, long duration) {
        super(id, RelicType.GUILD.toString(), duration);
        this.guildId = guildId.asLong();
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }
}
