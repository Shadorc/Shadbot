package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class ImageCmd extends BaseCmdGroup {

    public ImageCmd() {
        super(CommandCategory.IMAGE, "image", "Search random image on different sources",
                List.of(new DeviantartCmd(), new Rule34Cmd(), new SuicideGirlsCmd(), new WallhavenCmd()));
    }

}
