package com.shadorc.shadbot.db.guilds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Optional;

public class GuildsCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.guilds");

    public GuildsCollection(MongoDatabase database) {
        super(database.getCollection("guilds"));
    }

    public DBGuild getDBGuild(Snowflake guildId) {
        LOGGER.debug("[DBGuild {}] Request", guildId.asLong());

        final Document document = this.getCollection()
                .find(Filters.eq("_id", guildId.asString()))
                .first();

        if (document == null) {
            LOGGER.debug("[DBGuild {}] Not found.", guildId.asLong());
            return new DBGuild(guildId);
        } else {
            LOGGER.debug("[DBGuild {}] Found.", guildId.asLong());
            try {
                return new DBGuild(Utils.MAPPER.readValue(document.toJson(), DBGuildBean.class));
            } catch (final JsonProcessingException err) {
                throw new RuntimeException(err);
            }
        }
    }

    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        LOGGER.debug("[DBMember {} / {}] Request.", memberId.asLong(), guildId.asLong());

        final Optional<DBMember> member = this.getDBGuild(guildId)
                .getMembers()
                .stream()
                .filter(dbMember -> dbMember.getId().equals(memberId))
                .findFirst();

        if (member.isEmpty()) {
            LOGGER.debug("[DBMember {} / {}] Not found.", memberId.asLong(), guildId.asLong());
            return new DBMember(guildId, memberId);
        } else {
            LOGGER.debug("[DBMember {} / {}] Found.", memberId.asLong(), guildId.asLong());
            return member.get();
        }

    }

}
