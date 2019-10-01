package com.shadorc.shadbot.data.database;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Connection;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

public class DatabaseManagerV2 {

    private static DatabaseManagerV2 instance;

    static {
        try {
            DatabaseManagerV2.instance = new DatabaseManagerV2();
        } catch (final Exception err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", DatabaseManagerV2.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
    }

    private final RethinkDB db;
    private final Connection conn;
    private final Table table;

    public DatabaseManagerV2() {
        this.db = RethinkDB.r;
        this.conn = this.db.connection().hostname("localhost").port(28015).db("shadbot").connect();
        this.table = this.db.db("shadbot").table("guild");
    }

    public RethinkDB getDatabase() {
        return this.db;
    }

    public Connection getConnection() {
        return this.conn;
    }

    public DBGuild getDBGuild(Snowflake id) {
        try {
            return Utils.MAPPER.readValue(this.table.get(id.asLong()).toJson().toString(), DBGuild.class);
        } catch (final Exception err) {
            // TODO
            return null;
        }
    }

    public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
        try {
            //TODO
            return Utils.MAPPER.readValue(this.table.get(guildId.asLong()).toJson().toString(), DBMember.class);
        } catch (final Exception err) {
            // TODO
            return null;
        }
    }

    public static DatabaseManagerV2 getInstance() {
        return DatabaseManagerV2.instance;
    }

}
