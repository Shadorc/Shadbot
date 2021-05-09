package com.shadorc.shadbot.discord4j.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadorc.shadbot.command.standalone.StandaloneCmdsTest;
import discord4j.common.JacksonResources;

import java.io.IOException;

public class SerializationUtil {

    private static final ObjectMapper MAPPER = JacksonResources.INITIALIZER
            .andThen(JacksonResources.HANDLE_UNKNOWN_PROPERTIES)
            .apply(new ObjectMapper());

    public static <T> T read(String from, Class<T> into) throws IOException {
        return MAPPER.readValue(StandaloneCmdsTest.class.getClassLoader().getResourceAsStream(from), into);
    }


}
