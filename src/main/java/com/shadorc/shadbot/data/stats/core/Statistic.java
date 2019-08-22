package com.shadorc.shadbot.data.stats.core;

import com.shadorc.shadbot.data.stats.StatsManager;
import com.shadorc.shadbot.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class Statistic<E extends Enum<E>> {

    private final String fileName;
    private final Class<E> enumClass;

    protected Statistic(String fileName, Class<E> enumClass) {
        this.fileName = fileName;
        this.enumClass = enumClass;
    }

    public abstract Object getData();

    public void save() throws IOException {
        if (!this.getFile().getParentFile().exists() && !this.getFile().getParentFile().mkdirs()
                || !this.getFile().exists() && !this.getFile().createNewFile()) {
            throw new IOException("The folder or the file could not be created.");
        }
        try (final BufferedWriter writer = Files.newBufferedWriter(this.getFile().toPath())) {
            writer.write(Utils.MAPPER.writeValueAsString(this.getData()));
        }
    }

    public Class<E> getEnumClass() {
        return this.enumClass;
    }

    public File getFile() {
        return new File(StatsManager.STATS_DIR.getPath(), this.fileName);
    }

}
