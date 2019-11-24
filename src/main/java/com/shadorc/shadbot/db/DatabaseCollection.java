package com.shadorc.shadbot.db;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class DatabaseCollection {

    private final MongoCollection<Document> collection;

    protected DatabaseCollection(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public MongoCollection<Document> getCollection() {
        return this.collection;
    }

}
