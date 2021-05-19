package com.locibot.locibot.database.codec;

import com.locibot.locibot.database.guilds.bean.setting.AutoMessageBean;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class AutoMessageCodec implements Codec<AutoMessageBean> {

    @Override
    public AutoMessageBean decode(BsonReader reader, DecoderContext decoderContext) {
        try (reader) {
            reader.readStartDocument();
            return new AutoMessageBean(reader.readString(), reader.readString());
        }
    }

    @Override
    public void encode(BsonWriter writer, AutoMessageBean value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString("message", value.getMessage());
        writer.writeString("channel_id", value.getChannelId());
        writer.writeEndDocument();
    }

    @Override
    public Class<AutoMessageBean> getEncoderClass() {
        return AutoMessageBean.class;
    }
}
