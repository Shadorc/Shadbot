package com.shadorc.shadbot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bson.Document;

public interface DatabaseEntity {

    void insert();

    void delete();

    Document toDocument() throws JsonProcessingException;

}
