package com.shadorc.shadbot.api.json.gamestats.overwatch;

import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.Quickplay;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.gamestats.OverwatchCmd;

public record OverwatchProfile(OverwatchCmd.Platform platform,
                               ProfileResponse profile,
                               StatsResponse stats) {

    public Quickplay getQuickplay() {
        return this.stats.stats().topHeroes().quickplay();
    }

}
