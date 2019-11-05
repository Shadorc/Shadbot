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
                .user(Credentials.get(Credential.DATABASE_USER), Credentials.get(Credential.DATABASE_PASSWORD))
                .db(DATABASE_NAME)
                .connect();
        LogUtils.info("Connected to %s table.", this.tableName);
    }

    public RethinkDB getDatabase() {
        return DB;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public Table getTable() {
        return this.table;
    }

    public void close() {
        if (this.connection != null && this.connection.isOpen()) {
            this.connection.close();
            LogUtils.info("Connection to %s:%d/%s closed.", this.connection.hostname, this.connection.port, this.tableName);
        }
    }

}
