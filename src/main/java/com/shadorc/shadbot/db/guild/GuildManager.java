package com.shadorc.shadbot.db.guild;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

public class GuildManager extends DatabaseTable {

    private static GuildManager instance;

    static {
        try {
            GuildManager.instance = new GuildManager();
        } catch (final Exception err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", GuildManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
    }

    public GuildManager() {
        super("guild");
    }

    // TODO: Reduce DB calls
    public DBGuild getDBGuild(Snowflake guildId) {
        try (final Cursor<String> cursor = this.requestGuild(guildId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Utils.MAPPER.readValue(cursor.next(), DBGuild.class);
            } else {
                this.getTable().insert(this.getDatabase().hashMap("id", guildId.asLong())).run(this.getConnection());
                return new DBGuild(guildId);
            }

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting DBGuild.");
            return new DBGuild(guildId);
        }
    }

    // TODO: Reduce DB calls
    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        try (final Cursor<String> cursor = this.requestMember(guildId, memberId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Utils.MAPPER.readValue(cursor.next(), DBMember.class);
            } else {
                //TODO: Update or insert
                return new DBMember(guildId, memberId);
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting DBMember.");
            return new DBMember(guildId, memberId);
        }
    }

    public void deleteDBGuild(Snowflake guildId) {
        try {
            this.requestGuild(guildId).delete().run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while deleting DBGuild.");
        }
    }

    public void deleteDBMember(Snowflake guildId, Snowflake memberId) {
        try {
            // TODO: What if the member is not present ?
            this.requestMember(guildId, memberId).delete().run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while deleting DBMember.");
        }
    }

    private ReqlExpr requestGuild(Snowflake guildId) {
        return this.getTable().filter(this.getDatabase().hashMap("id", guildId.asLong()));
    }

    private ReqlExpr requestMember(Snowflake guildId, Snowflake memberId) {
        return this.getTable().filter(this.getDatabase().hashMap("id", guildId.asLong()))
                .filter(guild -> guild.hasFields("members"))
                .getField("members")
                .filter(members -> members.contains(this.getDatabase().hashMap("id", memberId.asLong())))
                .getField(this.getDatabase().hashMap("id", memberId.asLong()));
    }

    public static GuildManager getInstance() {
        return GuildManager.instance;
    }

}
