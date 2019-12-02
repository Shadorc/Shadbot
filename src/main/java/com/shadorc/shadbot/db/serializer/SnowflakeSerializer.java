package com.shadorc.shadbot.db.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import discord4j.core.object.util.Snowflake;

import java.lang.reflect.Type;

public class SnowflakeSerializer implements JsonSerializer<Snowflake> {

    @Override
    public JsonElement serialize(Snowflake src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.asString());
    }

}
