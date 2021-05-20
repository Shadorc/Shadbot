package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class ImageGroup extends BaseCmdGroup {

    public ImageGroup() {
        super(CommandCategory.IMAGE, "Search random image from different sources",
                List.of(new DeviantartCmd(), new SuicideGirlsCmd(), new WallhavenCmd(), new XkcdCmd()));
    }

}
