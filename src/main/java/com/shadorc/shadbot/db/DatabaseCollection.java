package com.shadorc.shadbot.db;

import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

public class DatabaseCollection {

    protected static final JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();

    private final MongoCollection<Document> collection;

    protected DatabaseCollection(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public MongoCollection<Document> getCollection() {
        return this.collection;
    }

}
