package com.locibot.locibot.command.gamestats;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class GameStatsGroup extends BaseCmdGroup {

    public GameStatsGroup() {
        super(CommandCategory.GAMESTATS, "gamestats", "Search game stats for different games",
                List.of(new OverwatchCmd(), new FortniteCmd(), new DiabloCmd(), new CounterStrikeCmd()));
    }

}
