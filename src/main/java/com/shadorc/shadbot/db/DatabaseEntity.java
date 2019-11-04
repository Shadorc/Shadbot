package com.shadorc.shadbot.db;

import java.io.IOException;

public interface DatabaseEntity {

    void readValue(String content) throws IOException;

    void insert();

    void delete();

}
