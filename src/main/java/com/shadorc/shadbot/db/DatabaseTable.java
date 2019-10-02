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

    private final String tableName;
    private final Table table;

    private Connection connection;

    protected DatabaseTable(String tableName) {
        this.tableName = tableName;
        this.table = DB.db(DATABASE_NAME).table(tableName);
    }

    public void connect() {
        this.connection = DB.connection()
                .hostname(Credentials.get(Credential.DATABASE_HOST))
                .port(Integer.parseInt(Credentials.get(Credential.DATABASE_PORT)))
                .user("admin", Credentials.get(Credential.DATABASE_PASSWORD))
                .db(DATABASE_NAME)
                .connect();
        LogUtils.info("Connected to %s table.", this.tableName);
    }

    // TODO: Set protected once migration is done
    public RethinkDB getDatabase() {
        return DB;
    }

    // TODO: Set protected once migration is done
    public Connection getConnection() {
        return this.connection;
    }

    protected Table getTable() {
        return this.table;
    }

    public void stop() {
        if (this.connection != null && this.connection.isOpen()) {
            this.connection.close();
            LogUtils.info("Connection to %s:%d/%s closed.", this.connection.hostname, this.connection.port, this.tableName);
        }
    }

}
