package com.shadorc.shadbot.database;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

public abstract class DatabaseCollection {

    protected static final JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();

    private final MongoCollection<Document> collection;
    private final String name;

    protected DatabaseCollection(MongoDatabase database, String name) {
        this.collection = database.getCollection(name);
        this.name = name;
    }

    public MongoCollection<Document> getCollection() {
        return this.collection;
    }

    public String getName() {
        return this.name;
    }

}
