package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class GameStatsCmd extends BaseCmdGroup {

    public GameStatsCmd() {
        super(CommandCategory.GAMESTATS, "gamestats", "Search game stats for different games",
                List.of(new OverwatchCmd(), new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd()));
    }

}
