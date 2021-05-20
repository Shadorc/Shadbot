package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class GameStatsGroupCmd extends GroupCmd {

    public GameStatsGroupCmd() {
        super(CommandCategory.GAMESTATS, "Search game stats for different games");
        this.addSubCommand(new OverwatchCmd(this));
        this.addSubCommand(new FortniteCmd(this));
        this.addSubCommand(new DiabloCmd(this));
        this.addSubCommand(new CounterStrikeCmd(this));
    }

}
