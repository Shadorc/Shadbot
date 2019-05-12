package me.shadorc.shadbot.data;

import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TextFile {

    private final List<String> texts;

    public TextFile(String path) {
        this.texts = new ArrayList<>();

        final File file = new File(path);
        if (file.exists()) {
            try {
                this.texts.addAll(Arrays.asList(Files.readString(file.toPath()).split("\n")));
            } catch (final IOException err) {
                LogUtils.error(err, String.format("An error occurred while reading text file: %s", file.getPath()));
            }
        }
    }

    public List<String> getTexts() {
        return Collections.unmodifiableList(this.texts);
    }

    public String getText() {
        return Utils.randValue(this.texts);
    }

}
