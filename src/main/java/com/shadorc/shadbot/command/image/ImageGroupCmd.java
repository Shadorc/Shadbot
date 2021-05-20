package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class ImageGroupCmd extends GroupCmd {

    public ImageGroupCmd() {
        super(CommandCategory.IMAGE, "Search random image from different sources");
        this.addSubCommand(new DeviantartCmd(this));
        this.addSubCommand(new SuicideGirlsCmd(this));
        this.addSubCommand(new WallhavenCmd(this));
        this.addSubCommand(new XkcdCmd(this));
    }

}
