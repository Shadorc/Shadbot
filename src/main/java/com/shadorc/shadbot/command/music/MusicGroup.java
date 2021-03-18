package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class MusicGroup extends BaseCmdGroup {

    public MusicGroup() {
        super(CommandCategory.MUSIC, "music", "Music commands",
                List.of(new BackwardCmd(), new BassBoostCmd(), new ClearCmd(), new ForwardCmd(), new NameCmd(),
                        new PauseCmd(), new PlaylistCmd(), new RepeatCmd(), new ShuffleCmd(), new SkipCmd(),
                        new StopCmd(), new VolumeCmd()));
    }

}
