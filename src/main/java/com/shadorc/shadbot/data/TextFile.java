package com.shadorc.shadbot.data;

import com.shadorc.shadbot.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class TextFile {

    private final List<String> texts;

    public TextFile(String path) {
        this.texts = new ArrayList<>();

        final File file = new File(path);
        if (file.exists()) {
            try {
                this.texts.addAll(Arrays.asList(Files.readString(file.toPath()).split("\n")));
            } catch (final IOException err) {
                DEFAULT_LOGGER.error(String.format("An error occurred while reading text file: %s", file.getPath()), err);
            }
        } else {
            DEFAULT_LOGGER.error("An error occurred while reading {}, this text file does not exist", file.getPath());
        }
    }

    public String getRandomText() {
        return Objects.requireNonNull(Utils.randValue(this.texts));
    }

    public String getRandomTextFormatted() {
        return Objects.requireNonNull(Utils.randValue(this.texts))
                .replace("{default_prefix}", Config.DEFAULT_PREFIX)
                .replace("{patreon_url}", Config.PATREON_URL)
                .replace("{support_server_url}", Config.SUPPORT_SERVER_URL);
    }

}
