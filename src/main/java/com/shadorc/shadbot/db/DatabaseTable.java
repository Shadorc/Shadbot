package com.shadorc.shadbot.db;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Connection;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;

public abstract class DatabaseTable {

    private static final String DATABASE_NAME = "shadbot";

    protected static final RethinkDB DB = RethinkDB.r;
    protected static final Connection CONNECTION = DB.connection()
            .hostname(Credentials.get(Credential.DATABASE_HOST))
            .port(Integer.parseInt(Credentials.get(Credential.DATABASE_PORT)))
            .user("admin", Credentials.get(Credential.DATABASE_PASSWORD))
            .db(DATABASE_NAME)
            .connect();

    protected final Table table;

    public DatabaseTable(String table) {
        this.table = RethinkDB.r.db(DATABASE_NAME).table(table);
    }

    public RethinkDB getDatabase() {
        return DB;
    }

    public Connection getConnection() {
        return CONNECTION;
    }

    public Table getTable() {
        return this.table;
    }

    public void stop() {
        if (this.getConnection().isOpen()) {
            this.getConnection().close();
        }
    }

}
