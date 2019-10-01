package com.shadorc.shadbot.data;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Connection;

public abstract class DatabaseTable {

    private static final String DATABASE_NAME = "shadbot";
    private static final String HOST = "localhost";
    private static final int PORT = 28015;

    protected static final RethinkDB DB = RethinkDB.r;
    protected static final Connection CONNECTION = DB.connection().hostname(HOST).port(PORT).db(DATABASE_NAME).connect();

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
