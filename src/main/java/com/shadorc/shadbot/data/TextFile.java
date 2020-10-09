package com.shadorc.shadbot.data;

import com.shadorc.shadbot.utils.RandUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class TextFile {

    private final List<String> lines;

    public TextFile(String path) {
        this.lines = new ArrayList<>();

        final File file = new File(path);
        if (file.exists()) {
            try {
                final String content = Files.readString(file.toPath()).replace("\r\n", "\n");
                this.lines.addAll(Arrays.asList(content.split("\n")));
            } catch (final IOException err) {
                DEFAULT_LOGGER.error(String.format("An error occurred while reading text file: %s", file.getPath()), err);
            }
        } else {
            DEFAULT_LOGGER.error("An error occurred while reading {}, this text file does not exist", file.getPath());
        }
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(this.lines);
    }

    public String getRandomLine() {
        return Objects.requireNonNull(RandUtils.randValue(this.lines));
    }

    public String getRandomLineFormatted() {
        return Objects.requireNonNull(RandUtils.randValue(this.lines))
                .replace("{default_prefix}", Config.DEFAULT_PREFIX)
                .replace("{patreon_url}", Config.PATREON_URL)
                .replace("{support_server_url}", Config.SUPPORT_SERVER_URL);
    }

}
