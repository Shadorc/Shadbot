package com.shadorc.shadbot.db.guild;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

public class GuildManager extends DatabaseTable {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.guild");

    private static GuildManager instance;

    static {
        GuildManager.instance = new GuildManager();
    }

    public GuildManager() {
        super("guild");
    }

    public DBGuild getDBGuild(Snowflake guildId) {
        LOGGER.debug("Requesting DBGuild with ID {}.", guildId.asLong());

        final DBGuild dbGuild = new DBGuild(guildId);
        try (final Cursor<String> cursor = this.requestGuild(guildId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                LOGGER.debug("DBGuild with ID {} found.", guildId.asLong());
                dbGuild.readValue(cursor.next());
                return dbGuild;
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while requesting DBGuild with ID %d.", guildId.asLong()));
        }
        LOGGER.debug("DBGuild with ID {} not found.", guildId.asLong());
        return dbGuild;
    }

    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        LOGGER.debug("Requesting DBMember with ID {}.", memberId.asLong());

        final DBMember dbMember = new DBMember(guildId, memberId);
        try (final Cursor<String> cursor = this.requestMember(guildId, memberId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                LOGGER.debug("DBMember with ID {} found.", memberId.asLong());
                dbMember.readValue(cursor.next());
                return dbMember;
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while requesting DBMember with ID %d, guild ID %d.",
                            memberId.asLong(), guildId.asLong()));
        }
        LOGGER.debug("DBMember with ID {} not found.", memberId.asLong());
        return dbMember;
    }

    public ReqlExpr requestGuild(Snowflake guildId) {
        return this.getTable()
                .filter(this.getDatabase().hashMap("id", guildId.asLong()));
    }

    public ReqlExpr requestMember(Snowflake guildId, Snowflake memberId) {
        return this.getTable()
                .filter(this.getDatabase().hashMap("id", guildId.asLong()))
                .filter(guild -> guild.hasFields("members"))
                .getField("members")
                .filter(this.getDatabase().hashMap("id", memberId.asLong()));
    }

    public static GuildManager getInstance() {
        return GuildManager.instance;
    }

}
