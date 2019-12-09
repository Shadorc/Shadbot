package com.shadorc.shadbot.db.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class LongCodec implements Codec<Long> {

    @Override
    public Long decode(BsonReader reader, DecoderContext decoderContext) {
        return Long.valueOf(reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, Long value, EncoderContext encoderContext) {
        writer.writeString(value.toString());
    }

    @Override
    public Class<Long> getEncoderClass() {
        return Long.class;
    }
}
