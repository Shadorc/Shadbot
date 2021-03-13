package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.utils.translate.TranslateCmd;
import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class UtilsCmd extends BaseCmdGroup {

    public UtilsCmd() {
        super(CommandCategory.UTILS, "utils", "Utility commands",
                List.of(new MathCmd(), new LyricsCmd(), new UrbanCmd(), new WeatherCmd(),
                        new WikipediaCmd(), new TranslateCmd()));
    }

}
