package com.shadorc.shadbot.database.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class LongCodec implements Codec<Long> {

    @Override
    public Long decode(BsonReader reader, DecoderContext decoderContext) {
        try (reader) {
            return reader.readInt64();
        }
    }

    @Override
    public void encode(BsonWriter writer, Long value, EncoderContext encoderContext) {
        writer.writeInt64(value);
    }

    @Override
    public Class<Long> getEncoderClass() {
        return Long.class;
    }
}
