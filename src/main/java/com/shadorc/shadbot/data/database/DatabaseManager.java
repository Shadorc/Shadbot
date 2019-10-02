package com.shadorc.shadbot.data.database;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.DatabaseTable;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

public class DatabaseManager extends DatabaseTable {

    private static DatabaseManager instance;

    static {
        try {
            DatabaseManager.instance = new DatabaseManager();
        } catch (final Exception err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", DatabaseManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
    }

    public DatabaseManager() {
        super("guild");
    }

    // TODO: Reduce DB calls
    public DBGuild getDBGuild(Snowflake id) {
        final ReqlExpr request = this.table
                .filter(DB.hashMap("id", id.asLong()))
                .map(ReqlExpr::toJson);

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Utils.MAPPER.readValue(cursor.next(), DBGuild.class);
            } else {
                this.table.insert(this.getDatabase().hashMap("id", id.asLong())).run(this.getConnection());
                return new DBGuild(id);
            }

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting DBGuild.");
            return new DBGuild(id);
        }
    }

    // TODO: Reduce DB calls
    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        final ReqlExpr request = this.table
                .filter(DB.hashMap("id", guildId.asLong()))
                .filter(guild -> guild.hasFields("members"))
                .getField("members")
                .filter(members -> members.contains(DB.hashMap("id", memberId.asLong())))
                .getField(DB.hashMap("id", memberId.asLong()));

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
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

    public void deleteDBGuild(Snowflake id) {
        try {
            this.table.filter(DB.hashMap("id", id.asLong())).delete().run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while deleting DBGuild.");
        }
    }

    public void deleteDBMember(Snowflake guildId, Snowflake memberId) {
        try {
            // TODO: What if the member is not present ?
            this.table.filter(DB.hashMap("id", guildId.asLong()))
                    .filter(guild -> guild.hasFields("members"))
                    .getField("members")
                    .filter(members -> members.contains(DB.hashMap("id", memberId.asLong())))
                    .getField(DB.hashMap("id", memberId.asLong()))
                    .delete()
                    .run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while deleting DBMember.");
        }
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
