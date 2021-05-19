package com.locibot.locibot.api.json.gamestats.overwatch;

import com.locibot.locibot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.locibot.locibot.api.json.gamestats.overwatch.stats.Quickplay;
import com.locibot.locibot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.locibot.locibot.command.gamestats.OverwatchCmd;

public record OverwatchProfile(OverwatchCmd.Platform platform,
                               ProfileResponse profile,
                               StatsResponse stats) {

    public Quickplay getQuickplay() {
        return this.stats.stats().topHeroes().quickplay();
    }

}
