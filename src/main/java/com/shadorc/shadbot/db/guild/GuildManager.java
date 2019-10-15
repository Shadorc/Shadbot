package com.shadorc.shadbot.db.guild;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

public class GuildManager extends DatabaseTable {

    private static GuildManager instance;

    static {
        GuildManager.instance = new GuildManager();
    }

    public GuildManager() {
        super("guild");
    }

    // TODO: Reduce DB calls
    public DBGuild getDBGuild(Snowflake guildId) {
        try (final Cursor<String> cursor = this.requestGuild(guildId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Utils.MAPPER.readValue(cursor.next(), DBGuild.class);
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while getting DBGuild with ID %d.", guildId.asLong()));
        }
        return new DBGuild(guildId);
    }

    // TODO: Reduce DB calls
    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        try (final Cursor<String> cursor = this.requestMember(guildId, memberId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Utils.MAPPER.readValue(cursor.next(), DBMember.class);
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while getting DBMember with ID %d, guild ID %d.",
                            memberId.asLong(), guildId.asLong()));
        }
        return new DBMember(guildId, memberId);
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
                .filter(members -> members.contains(this.getDatabase().hashMap("id", memberId.asLong())))
                .getField(this.getDatabase().hashMap("id", memberId.asLong()));
    }

    public static GuildManager getInstance() {
        return GuildManager.instance;
    }

}
