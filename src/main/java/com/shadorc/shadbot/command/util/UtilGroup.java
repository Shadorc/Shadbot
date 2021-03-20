package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.command.util.translate.TranslateCmd;
import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class UtilGroup extends BaseCmdGroup {

    public UtilGroup() {
        super(CommandCategory.UTILS, "util", "Utility commands",
                List.of(new MathCmd(), new LyricsCmd(), new UrbanCmd(), new WeatherCmd(), new WikipediaCmd(),
                        new TranslateCmd()));
    }

}
