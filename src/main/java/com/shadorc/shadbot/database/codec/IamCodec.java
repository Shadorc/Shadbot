package com.shadorc.shadbot.database.codec;

import com.shadorc.shadbot.database.guilds.bean.setting.IamBean;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class IamCodec implements Codec<IamBean> {

    @Override
    public IamBean decode(BsonReader reader, DecoderContext decoderContext) {
        try (reader) {
            reader.readStartDocument();
            return new IamBean(reader.readString(), reader.readString());
        }
    }

    @Override
    public void encode(BsonWriter writer, IamBean value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString("message_id", value.getMessageId());
        writer.writeString("role_id", value.getRoleId());
        writer.writeEndDocument();
    }

    @Override
    public Class<IamBean> getEncoderClass() {
        return IamBean.class;
    }

}
