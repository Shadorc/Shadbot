package com.locibot.locibot.command.image;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class ImageGroup extends BaseCmdGroup {

    public ImageGroup() {
        super(CommandCategory.IMAGE, "image", "Search random image from different sources",
                List.of(new DeviantartCmd(), new SuicideGirlsCmd(), new WallhavenCmd(), new XkcdCmd()));
    }

}
