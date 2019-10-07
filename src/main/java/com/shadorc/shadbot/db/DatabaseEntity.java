package com.shadorc.shadbot.db;

public abstract class DatabaseEntity {

    protected abstract void update(String field, Object value);

    public abstract void delete();

}
