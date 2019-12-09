package com.shadorc.shadbot.db.codec;

import discord4j.core.object.util.Snowflake;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class SnowflakeCodec implements Codec<Snowflake> {

    @Override
    public Snowflake decode(BsonReader reader, DecoderContext decoderContext) {
        return Snowflake.of(reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, Snowflake value, EncoderContext encoderContext) {
        writer.writeString(value.asString());
    }

    @Override
    public Class<Snowflake> getEncoderClass() {
        return Snowflake.class;
    }

}
