package com.shadorc.shadbot.db;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Connection;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.utils.LogUtils;

public class DatabaseTable {

    private static final String DATABASE_NAME = "shadbot";

    private static final RethinkDB DB = RethinkDB.r;
    private static final Connection CONNECTION = DB.connection()
            .hostname(Credentials.get(Credential.DATABASE_HOST))
            .port(Integer.parseInt(Credentials.get(Credential.DATABASE_PORT)))
            .user("admin", Credentials.get(Credential.DATABASE_PASSWORD))
            .db(DATABASE_NAME)
            .connect();

    private final Table table;

    protected DatabaseTable(String table) {
        this.table = DB.db(DATABASE_NAME).table(table);
    }

    // TODO: Set protected once migration is done
    public RethinkDB getDatabase() {
        return DB;
    }

    // TODO: Set protected once migration is done
    public Connection getConnection() {
        return CONNECTION;
    }

    protected Table getTable() {
        return this.table;
    }

    public void stop() {
        if (this.getConnection().isOpen()) {
            this.getConnection().close();
            LogUtils.info("Connection to %s:%d closed.", this.getConnection().hostname, this.getConnection().port);
        }
    }

}
