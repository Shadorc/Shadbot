package com.shadorc.shadbot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shadorc.shadbot.utils.Utils;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;

public abstract class DatabaseEntity<T extends Bean> {

    private final T bean;

    public DatabaseEntity(T bean) {
        this.bean = bean;
    }

    public T getBean() {
        return this.bean;
    }

    public abstract void insert();

    public abstract void delete();

    public Document toDocument() {
        try {
            return Document.parse(Utils.MAPPER.writeValueAsString(this.getBean()),
                    new DocumentCodec(Utils.CODEC_REGISTRY));
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

}
