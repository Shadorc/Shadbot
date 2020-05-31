package com.shadorc.shadbot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shadorc.shadbot.utils.NetUtils;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;

public abstract class SerializableEntity<T extends Bean> {

    private final T bean;

    public SerializableEntity(T bean) {
        this.bean = bean;
    }

    public T getBean() {
        return this.bean;
    }

    public Document toDocument() {
        try {
            return Document.parse(NetUtils.MAPPER.writeValueAsString(this.getBean()),
                    new DocumentCodec(DatabaseManager.CODEC_REGISTRY));
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public String toString() {
        return "SerializableEntity{" +
                "bean=" + this.bean +
                '}';
    }
}
