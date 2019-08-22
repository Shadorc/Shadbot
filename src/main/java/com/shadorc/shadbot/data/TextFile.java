package com.shadorc.shadbot.data;

import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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

    public String getText() {
        return Utils.randValue(this.texts);
    }

}
