package com.locibot.locibot.data;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.utils.RandUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
                LociBot.DEFAULT_LOGGER.error("An error occurred while reading text file: %s".formatted(file.getPath()), err);
            }
        } else {
            LociBot.DEFAULT_LOGGER.error("Text file {} not found", file.getPath());
        }
    }

    public String getRandomLine() {
        return Objects.requireNonNull(RandUtil.randValue(this.lines));
    }

    public String getRandomLineFormatted() {
        return Objects.requireNonNull(RandUtil.randValue(this.lines))
                .replace("{patreon_url}", Config.PATREON_URL)
                .replace("{support_server_url}", Config.SUPPORT_SERVER_URL);
    }

}
