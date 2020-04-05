package com.shadorc.shadbot.api.json.gamestats.overwatch;

import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.Quickplay;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.gamestats.OverwatchCmd;

public class OverwatchProfile {

    private final OverwatchCmd.Platform platform;
    private final ProfileResponse profile;
    private final StatsResponse stats;

    public OverwatchProfile(OverwatchCmd.Platform platform, ProfileResponse profile, StatsResponse stats) {
        this.platform = platform;
        this.profile = profile;
        this.stats = stats;
    }

    public OverwatchCmd.Platform getPlatform() {
        return this.platform;
    }

    public ProfileResponse getProfile() {
        return this.profile;
    }

    public Quickplay getQuickplay() {
        return this.stats.getStats().getTopHeroes().getQuickplay();
    }

    @Override
    public String toString() {
        return "OverwatchProfile{" +
                "platform=" + this.platform +
                ", profile=" + this.profile +
                ", stats=" + this.stats +
                '}';
    }
}
