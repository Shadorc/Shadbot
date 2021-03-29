package com.shadorc.shadbot.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shadorc.shadbot.utils.NetUtil;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;

public abstract class SerializableEntity<T extends Bean> {

    private static final DocumentCodec DECODER = new DocumentCodec(DatabaseManager.CODEC_REGISTRY);

    private final T bean;

    public SerializableEntity(T bean) {
        this.bean = bean;
    }

    public T getBean() {
        return this.bean;
    }

    public Document toDocument() {
        try {
            return Document.parse(NetUtil.MAPPER.writeValueAsString(this.getBean()), DECODER);
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
